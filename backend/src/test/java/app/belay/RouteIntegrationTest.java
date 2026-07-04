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

/**
 * Chemin critique de la Phase 4 (voies & murs) : secteurs et voies avec photos signées,
 * annotations de prises (aller-retour), matrice de permissions, anti-IDOR inter-organisations.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouteIntegrationTest {

    @LocalServerPort
    private int port;

    private String tag;
    private ApiActor owner;
    private ApiActor coach;
    private ApiActor member;

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
        owner.post("/api/members/" + id + "/approve", null, JsonNode.class);
        return id;
    }

    @Test
    void wallMapCoversSectorsRoutesPhotosAndHoldAnnotations() throws Exception {
        // L'admin crée un secteur avec photo de mur ; le moniteur y crée une voie avec photo
        String sectorId =
                createSector(owner, "Dévers", true).getBody().path("id").asText();
        ResponseEntity<JsonNode> route = createRoute(coach, sectorId, "La bleue", "6a+", true);
        assertThat(route.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String routeId = route.getBody().path("id").asText();
        assertThat(route.getBody().path("photoUrl").asText()).startsWith("/api/media/routes/");

        // Le moniteur annote les prises (coordonnées relatives) — rendues en overlay côté front
        ResponseEntity<JsonNode> annotated = coach.put(
                "/api/routes/" + routeId + "/holds",
                Map.of("holds", List.of(Map.of("x", 0.25, "y", 0.8), Map.of("x", 0.5, "y", 0.55))),
                JsonNode.class);
        assertThat(annotated.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Un membre voit la cartographie complète : secteur, photo signée, voie, prises
        JsonNode sectors = member.get("/api/sectors", JsonNode.class).getBody();
        JsonNode sector = sectors.get(0);
        assertThat(sector.path("name").asText()).isEqualTo("Dévers");
        assertThat(sector.path("photoUrl").asText()).startsWith("/api/media/sectors/");
        JsonNode routeItem = sector.path("routes").get(0);
        assertThat(routeItem.path("name").asText()).isEqualTo("La bleue");
        assertThat(routeItem.path("grade").asText()).isEqualTo("6a+");
        assertThat(routeItem.path("holds")).hasSize(2);
        assertThat(routeItem.path("holds").get(0).path("x").asDouble()).isEqualTo(0.25);

        // Coordonnées hors bornes → 400
        assertThat(coach.put(
                                "/api/routes/" + routeId + "/holds",
                                Map.of("holds", List.of(Map.of("x", 1.4, "y", 0.5))),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void wallMapFollowsThePermissionMatrix() {
        String sectorId =
                createSector(owner, "Dalle", false).getBody().path("id").asText();

        // Un moniteur ne gère pas les secteurs ; un membre ne crée pas de voie
        assertThat(createSector(coach, "Interdit", false).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(createRoute(member, sectorId, "Interdit", "5c", false).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Un secteur non vide ne se supprime pas ; vidé, si
        String routeId = createRoute(coach, sectorId, "La rouge", "7a", false)
                .getBody()
                .path("id")
                .asText();
        assertThat(owner.delete("/api/sectors/" + sectorId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // L'admin peut supprimer la voie d'un moniteur (créateur ou admin)
        assertThat(owner.delete("/api/routes/" + routeId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(owner.delete("/api/sectors/" + sectorId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void wallMapIsIsolatedBetweenOrganizations() {
        String sectorId = createSector(owner, "Interne " + tag, false)
                .getBody()
                .path("id")
                .asText();
        String routeId = createRoute(coach, sectorId, "Voie interne", "6b", false)
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

        // Lecture : la cartographie de B est vide
        assertThat(ownerB.get("/api/sectors", JsonNode.class).getBody()).isEmpty();

        // Écriture : créer une voie dans le secteur de A, annoter ou supprimer → 404
        assertThat(createRoute(ownerB, sectorId, "Intrusion", "6a", false).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.put(
                                "/api/routes/" + routeId + "/holds",
                                Map.of("holds", List.of(Map.of("x", 0.5, "y", 0.5))),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.delete("/api/routes/" + routeId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(member.get("/api/sectors", JsonNode.class).getBody().get(0).path("routes"))
                .hasSize(1);
    }

    private ResponseEntity<JsonNode> createSector(ApiActor actor, String name, boolean withPhoto) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("meta", Map.of("name", name), MediaType.APPLICATION_JSON);
        if (withPhoto) {
            builder.part("photo", namedResource(pngBytes(), "mur.png"), MediaType.IMAGE_PNG);
        }
        return actor.postMultipart("/api/sectors", builder.build(), JsonNode.class);
    }

    private ResponseEntity<JsonNode> createRoute(
            ApiActor actor, String sectorId, String name, String grade, boolean withPhoto) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part(
                "meta",
                Map.of("sectorId", sectorId, "name", name, "grade", grade, "climbType", "BOULDER"),
                MediaType.APPLICATION_JSON);
        if (withPhoto) {
            builder.part("photo", namedResource(pngBytes(), "voie.png"), MediaType.IMAGE_PNG);
        }
        return actor.postMultipart("/api/routes", builder.build(), JsonNode.class);
    }

    private static ByteArrayResource namedResource(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static byte[] pngBytes() {
        try {
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
