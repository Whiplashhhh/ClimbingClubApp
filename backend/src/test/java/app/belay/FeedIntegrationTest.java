package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * Chemin critique de la Phase 2 : publication (matrice de permissions par audience), agrégation
 * du fil, upload d'affiche (validation par contenu + URL signée), suppression, et isolation
 * inter-organisations (anti-IDOR).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedIntegrationTest {

    @LocalServerPort
    private int port;

    private String tag;
    private ApiActor owner;
    private ApiActor coach;
    private ApiActor member;
    private String coachId;

    /** Org avec un OWNER, un COACH et un MEMBER actifs — recréée pour chaque test. */
    @BeforeEach
    void setUpOrganization() {
        tag = UUID.randomUUID().toString().substring(0, 8);
        owner = new ApiActor(port);
        String slug = owner.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "owner-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Owner",
                                "createOrganization",
                                Map.of("name", "Club " + tag, "climbingType", "BOTH")),
                        JsonNode.class)
                .getBody()
                .path("organization")
                .path("slug")
                .asText();

        coach = new ApiActor(port);
        coachId = joinAndApprove(coach, "coach-" + tag + "@club.fr", "Coach", slug);
        owner.patch("/api/members/" + coachId + "/role", Map.of("role", "COACH"), JsonNode.class);

        member = new ApiActor(port);
        joinAndApprove(member, "member-" + tag + "@club.fr", "Member", slug);
    }

    private String joinAndApprove(ApiActor actor, String email, String displayName, String slug) {
        String id = actor.post(
                        "/api/auth/register",
                        Map.of(
                                "email", email,
                                "password", "s3cure-password",
                                "displayName", displayName,
                                "joinSlug", slug),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        assertThat(owner.post("/api/members/" + id + "/approve", null, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return id;
    }

    @Test
    void feedAggregatesPostsByAudienceAndEnforcesPublishingMatrix() {
        // OWNER publie à toute l'org ; COACH publie à ses élèves
        assertThat(createPost(owner, "INFO", "ORG", "Assemblée générale").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(createPost(coach, "CANCELLATION", "COACH_STUDENTS", "Cours annulé jeudi")
                        .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Le membre voit le post org-entière mais pas celui du coach (pas encore son élève — A-005)
        List<String> memberTitles = feedTitles(member);
        assertThat(memberTitles).contains("Assemblée générale").doesNotContain("Cours annulé jeudi");

        // Le coach voit son propre post et le post org-entière
        assertThat(feedTitles(coach)).contains("Assemblée générale", "Cours annulé jeudi");

        // Matrice de permissions : rejets serveur en 403
        assertThat(createPost(member, "INFO", "ORG", "Interdit").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(createPost(coach, "INFO", "ORG", "Interdit").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(createPost(owner, "INFO", "COACH_STUDENTS", "Interdit").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Le type POSTER ne passe pas par l'endpoint JSON
        assertThat(createPost(owner, "POSTER", "ORG", "Interdit").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void posterUploadValidatesContentAndServesSignedUrl() throws Exception {
        // Une vraie image PNG passe
        MultipartBodyBuilder poster = new MultipartBodyBuilder();
        poster.part("meta", Map.of("audience", "ORG", "title", "Affiche compétition"), MediaType.APPLICATION_JSON);
        poster.part("image", namedResource(pngBytes(), "poster.png"), MediaType.IMAGE_PNG);
        ResponseEntity<JsonNode> created = owner.postMultipart("/api/posts/poster", poster.build(), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String imageUrl = created.getBody().path("imageUrl").asText();
        assertThat(imageUrl).contains("X-Amz-Signature");

        // L'URL signée sert bien l'image, avec le bon Content-Type détecté par contenu.
        // URI.create : ne pas ré-encoder l'URL signée (la signature couvre la query string).
        ResponseEntity<byte[]> download = new RestTemplate().getForEntity(java.net.URI.create(imageUrl), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(download.getBody()).isEqualTo(pngBytes());

        // Le membre voit l'affiche dans son fil avec une URL signée
        JsonNode memberFeed = member.get("/api/feed", JsonNode.class).getBody();
        JsonNode posterItem = memberFeed.path("items").get(0);
        assertThat(posterItem.path("title").asText()).isEqualTo("Affiche compétition");
        assertThat(posterItem.path("imageUrl").asText()).contains("X-Amz-Signature");

        // Un fichier non-image est rejeté par sniffing du contenu, même nommé .png
        MultipartBodyBuilder fake = new MultipartBodyBuilder();
        fake.part("meta", Map.of("audience", "ORG", "title", "Pas une image"), MediaType.APPLICATION_JSON);
        fake.part("image", namedResource("<script>alert(1)</script>".getBytes(), "evil.png"), MediaType.IMAGE_PNG);
        assertThat(owner.postMultipart("/api/posts/poster", fake.build(), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteIsRestrictedToAuthorOrAdmins() {
        String coachPostId = createPost(coach, "INFO", "COACH_STUDENTS", "Info du coach")
                .getBody()
                .path("id")
                .asText();

        // Un simple membre ne peut pas supprimer le post d'un autre
        assertThat(member.delete("/api/posts/" + coachPostId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // L'auteur peut supprimer son post ; un admin aussi
        assertThat(coach.delete("/api/posts/" + coachPostId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        String ownerPostId = createPost(owner, "INFO", "ORG", "Info org")
                .getBody()
                .path("id")
                .asText();
        assertThat(owner.delete("/api/posts/" + ownerPostId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(feedTitles(owner)).doesNotContain("Info du coach", "Info org");
    }

    @Test
    void postsAreIsolatedBetweenOrganizations() {
        String postId = createPost(owner, "INFO", "ORG", "Interne au club " + tag)
                .getBody()
                .path("id")
                .asText();

        ApiActor ownerB = new ApiActor(port);
        ownerB.post(
                "/api/auth/register",
                Map.of(
                        "email",
                        "owner-b-" + tag + "@club.fr",
                        "password",
                        "s3cure-password",
                        "displayName",
                        "Owner B",
                        "createOrganization",
                        Map.of("name", "Autre club " + tag, "climbingType", "BOULDER")),
                JsonNode.class);

        // Lecture : le fil de B n'agrège jamais les posts de A
        assertThat(feedTitles(ownerB)).doesNotContain("Interne au club " + tag);

        // Écriture : suppression inter-org → 404 (existence masquée), le post de A est intact
        assertThat(ownerB.delete("/api/posts/" + postId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(feedTitles(member)).contains("Interne au club " + tag);
    }

    private ResponseEntity<JsonNode> createPost(ApiActor actor, String type, String audience, String title) {
        return actor.post(
                "/api/posts",
                Map.of("type", type, "audience", audience, "title", title, "body", "corps du message"),
                JsonNode.class);
    }

    private List<String> feedTitles(ApiActor actor) {
        JsonNode feed = actor.get("/api/feed", JsonNode.class).getBody();
        return feed.path("items").findValuesAsText("title");
    }

    private static ByteArrayResource namedResource(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
