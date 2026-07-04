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
 * Chemin critique du fil : publication (matrice de permissions par audience), agrégation,
 * pièces jointes multi-images (validation par contenu + URLs signées), suppression, et
 * isolation inter-organisations (anti-IDOR).
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
        String coachId = joinAndApprove(coach, "coach-" + tag + "@club.fr", "Coach", slug);
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
        assertThat(createPost(owner, "ORG", "Assemblée générale").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(createPost(coach, "COACH_STUDENTS", "Info du coach").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Le membre voit le post org-entière mais pas celui du coach (pas encore son élève — A-005)
        List<String> memberTitles = feedTitles(member);
        assertThat(memberTitles).contains("Assemblée générale").doesNotContain("Info du coach");

        // Le coach voit son propre post et le post org-entière
        assertThat(feedTitles(coach)).contains("Assemblée générale", "Info du coach");

        // Matrice de permissions : rejets serveur en 403
        assertThat(createPost(member, "ORG", "Interdit").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(createPost(coach, "ORG", "Interdit").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(createPost(owner, "COACH_STUDENTS", "Interdit").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void postsCarryContentSniffedImagesServedThroughSignedUrls() throws Exception {
        // Deux vraies images passent, dans l'ordre
        MultipartBodyBuilder withImages = metaPart("ORG", "Photos du mur");
        withImages.part("images", namedResource(pngBytes(), "un.png"), MediaType.IMAGE_PNG);
        withImages.part("images", namedResource(jpegBytes(), "deux.jpg"), MediaType.IMAGE_JPEG);
        ResponseEntity<JsonNode> created = owner.postMultipart("/api/posts", withImages.build(), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode urls = created.getBody().path("imageUrls");
        assertThat(urls).hasSize(2);
        assertThat(urls.get(0).asText()).contains("X-Amz-Signature");

        // L'URL signée sert bien la première image (URI.create : ne pas ré-encoder la signature)
        ResponseEntity<byte[]> download =
                new RestTemplate().getForEntity(java.net.URI.create(urls.get(0).asText()), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);

        // Le membre voit le post et ses deux images dans son fil
        JsonNode feedItem =
                member.get("/api/feed", JsonNode.class).getBody().path("items").get(0);
        assertThat(feedItem.path("title").asText()).isEqualTo("Photos du mur");
        assertThat(feedItem.path("imageUrls")).hasSize(2);

        // Un fichier non-image est rejeté par sniffing du contenu, même nommé .png
        MultipartBodyBuilder fake = metaPart("ORG", "Pas une image");
        fake.part("images", namedResource("<script>alert(1)</script>".getBytes(), "evil.png"), MediaType.IMAGE_PNG);
        assertThat(owner.postMultipart("/api/posts", fake.build(), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Plus de 4 images → 400
        MultipartBodyBuilder tooMany = metaPart("ORG", "Trop d'images");
        for (int i = 0; i < 5; i++) {
            tooMany.part("images", namedResource(pngBytes(), "img" + i + ".png"), MediaType.IMAGE_PNG);
        }
        assertThat(owner.postMultipart("/api/posts", tooMany.build(), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteIsRestrictedToAuthorOrAdmins() {
        String coachPostId = createPost(coach, "COACH_STUDENTS", "Info du coach")
                .getBody()
                .path("id")
                .asText();

        // Un simple membre ne peut pas supprimer le post d'un autre
        assertThat(member.delete("/api/posts/" + coachPostId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // L'auteur peut supprimer son post ; un admin aussi
        assertThat(coach.delete("/api/posts/" + coachPostId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        String ownerPostId =
                createPost(owner, "ORG", "Info org").getBody().path("id").asText();
        assertThat(owner.delete("/api/posts/" + ownerPostId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(feedTitles(owner)).doesNotContain("Info du coach", "Info org");
    }

    @Test
    void postsAreIsolatedBetweenOrganizations() {
        String postId = createPost(owner, "ORG", "Interne au club " + tag)
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

    private MultipartBodyBuilder metaPart(String audience, String title) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part(
                "meta",
                Map.of("audience", audience, "title", title, "body", "corps du message"),
                MediaType.APPLICATION_JSON);
        return builder;
    }

    private ResponseEntity<JsonNode> createPost(ApiActor actor, String audience, String title) {
        return actor.postMultipart("/api/posts", metaPart(audience, title).build(), JsonNode.class);
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

    private static byte[] jpegBytes() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
