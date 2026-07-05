package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;

/**
 * Chemin critique de la Phase 5A (séances & ascensions) : lancer une séance, ajouter des
 * ascensions sur une voie du club, fil d'activité respectant la confidentialité, et isolation
 * inter-organisations (anti-IDOR).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionIntegrationTest {

    @LocalServerPort
    private int port;

    private String tag;
    private ApiActor owner;
    private ApiActor member;
    private String routeId;

    @BeforeEach
    void setUp() {
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
        member = new ApiActor(port);
        String memberId = member.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "member-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Member",
                                "joinSlug",
                                slug),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        owner.post("/api/members/" + memberId + "/approve", null, JsonNode.class);

        // Une voie pour porter les ascensions
        MultipartBodyBuilder sector = new MultipartBodyBuilder();
        sector.part("meta", Map.of("name", "Dévers"), MediaType.APPLICATION_JSON);
        String sectorId = owner.postMultipart("/api/sectors", sector.build(), JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        MultipartBodyBuilder route = new MultipartBodyBuilder();
        route.part(
                "meta",
                Map.of("sectorId", sectorId, "name", "La bleue", "grade", "6a+", "climbType", "BOULDER"),
                MediaType.APPLICATION_JSON);
        routeId = owner.postMultipart("/api/routes", route.build(), JsonNode.class)
                .getBody()
                .path("id")
                .asText();
    }

    @Test
    void memberLogsASessionWithAscentsAndSeesItInTheHistory() {
        String sessionId = owner.post(
                        "/api/sessions", Map.of("note", "Bonne séance", "visibility", "PRIVATE"), JsonNode.class)
                .getBody()
                .path("id")
                .asText();

        Map<String, Object> ascent = new HashMap<>();
        ascent.put("routeId", routeId);
        ascent.put("rating", 4);
        ascent.put("topHold", 8);
        ascent.put("durationSeconds", 95);
        ascent.put("belayerName", "Marie");
        ResponseEntity<JsonNode> withAscent =
                owner.post("/api/sessions/" + sessionId + "/ascents", ascent, JsonNode.class);
        assertThat(withAscent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode a = withAscent.getBody().path("ascents").get(0);
        assertThat(a.path("routeName").asText()).isEqualTo("La bleue");
        assertThat(a.path("rating").asInt()).isEqualTo(4);
        assertThat(a.path("topHold").asInt()).isEqualTo(8);
        assertThat(a.path("belayerName").asText()).isEqualTo("Marie");

        // Validation : appréciation hors 1..5 → 400
        assertThat(owner.post(
                                "/api/sessions/" + sessionId + "/ascents",
                                Map.of("routeId", routeId, "rating", 9),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Historique perso
        JsonNode mine = owner.get("/api/sessions/mine", JsonNode.class).getBody();
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).path("ascents")).hasSize(1);

        // Retirer l'ascension
        String ascentId = a.path("id").asText();
        assertThat(owner.delete("/api/sessions/" + sessionId + "/ascents/" + ascentId, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(owner.get("/api/sessions/mine", JsonNode.class)
                        .getBody()
                        .get(0)
                        .path("ascents"))
                .isEmpty();
    }

    @Test
    void clubActivityRespectsVisibility() {
        // Le membre a une séance PRIVATE et une séance CLUB
        member.post("/api/sessions", Map.of("note", "Perso", "visibility", "PRIVATE"), JsonNode.class);
        member.post("/api/sessions", Map.of("note", "Partagée", "visibility", "CLUB"), JsonNode.class);
        // FRIENDS équivaut à privé tant que le graphe d'amis n'existe pas (A-012)
        member.post("/api/sessions", Map.of("note", "Amis", "visibility", "FRIENDS"), JsonNode.class);

        // L'owner voit la séance CLUB du membre, pas la PRIVATE ni la FRIENDS
        List<String> ownerActivity =
                owner.get("/api/sessions/club", JsonNode.class).getBody().findValuesAsText("note");
        assertThat(ownerActivity).contains("Partagée").doesNotContain("Perso", "Amis");

        // Le membre voit ses trois séances dans son propre fil d'activité
        assertThat(member.get("/api/sessions/club", JsonNode.class).getBody().findValuesAsText("note"))
                .contains("Perso", "Partagée", "Amis");
    }

    @Test
    void sessionsAreIsolatedBetweenOrganizations() {
        String sessionId = member.post("/api/sessions", Map.of("visibility", "CLUB"), JsonNode.class)
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

        // Lecture : le fil d'activité de B ne contient jamais les séances de A
        assertThat(ownerB.get("/api/sessions/club", JsonNode.class).getBody()).isEmpty();

        // Écriture : modifier, supprimer ou ajouter une ascension à une séance de A → 404
        assertThat(ownerB.patch("/api/sessions/" + sessionId, Map.of("visibility", "PRIVATE"), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.post("/api/sessions/" + sessionId + "/ascents", Map.of("routeId", routeId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.delete("/api/sessions/" + sessionId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Un membre ne peut pas modifier la séance d'un autre membre de SON club (403)
        String ownerSessionId = owner.post("/api/sessions", Map.of("visibility", "CLUB"), JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        assertThat(member.delete("/api/sessions/" + ownerSessionId, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
