package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;

/**
 * Chemin critique de la Phase 5B : graphe d'amis (demande → acceptation), visibilité FRIENDS des
 * séances réservée aux amis, assureur choisi parmi les membres, et isolation inter-organisations
 * (anti-IDOR sur la demande d'ami).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FriendIntegrationTest {

    @LocalServerPort
    private int port;

    private String tag;
    private ApiActor owner;
    private ApiActor alice;
    private ApiActor bob;
    private UUID aliceId;
    private UUID bobId;
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

        alice = new ApiActor(port);
        aliceId = UUID.fromString(register(alice, "alice", "Alice", slug));
        bob = new ApiActor(port);
        bobId = UUID.fromString(register(bob, "bob", "Bob", slug));
        owner.post("/api/members/" + aliceId + "/approve", null, JsonNode.class);
        owner.post("/api/members/" + bobId + "/approve", null, JsonNode.class);

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

    private String register(ApiActor actor, String local, String displayName, String slug) {
        return actor.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                local + "-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                displayName,
                                "joinSlug",
                                slug),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
    }

    @Test
    void friendRequestFlowAndSharedFriendsSessions() {
        // Alice envoie une demande à Bob
        assertThat(alice.post("/api/friends/requests", Map.of("addresseeId", bobId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Demande en double → 409
        assertThat(alice.post("/api/friends/requests", Map.of("addresseeId", bobId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // Bob voit la demande entrante ; Alice n'en a aucune
        assertThat(bob.get("/api/friends/requests/incoming", JsonNode.class)
                        .getBody()
                        .findValuesAsText("displayName"))
                .contains("Alice");
        assertThat(alice.get("/api/friends/requests/incoming", JsonNode.class).getBody())
                .isEmpty();

        // Alice (demandeuse) ne peut pas accepter sa propre demande → 403
        assertThat(alice.post("/api/friends/" + bobId + "/accept", null, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Bob accepte → 204
        assertThat(bob.post("/api/friends/" + aliceId + "/accept", null, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        // Chacun voit l'autre dans sa liste d'amis
        assertThat(alice.get("/api/friends", JsonNode.class).getBody().findValuesAsText("displayName"))
                .containsExactly("Bob");
        assertThat(bob.get("/api/friends", JsonNode.class).getBody().findValuesAsText("displayName"))
                .containsExactly("Alice");

        // Bob publie une séance FRIENDS ; l'owner (non ami) publie aussi une séance FRIENDS
        bob.post("/api/sessions", Map.of("note", "Séance de Bob", "visibility", "FRIENDS"), JsonNode.class);
        owner.post("/api/sessions", Map.of("note", "Séance owner", "visibility", "FRIENDS"), JsonNode.class);

        // Alice voit la séance FRIENDS de son ami Bob, pas celle de l'owner non ami
        assertThat(alice.get("/api/sessions/club", JsonNode.class).getBody().findValuesAsText("note"))
                .contains("Séance de Bob")
                .doesNotContain("Séance owner");
    }

    @Test
    void ascentBelayerCanBeAClubMember() {
        String sessionId = owner.post("/api/sessions", Map.of("visibility", "PRIVATE"), JsonNode.class)
                .getBody()
                .path("id")
                .asText();

        JsonNode ascent = owner.post(
                        "/api/sessions/" + sessionId + "/ascents",
                        Map.of("routeId", routeId, "rating", 4, "belayerUserId", aliceId),
                        JsonNode.class)
                .getBody()
                .path("ascents")
                .get(0);
        // L'assureur membre prime : son displayName est renvoyé
        assertThat(ascent.path("belayerName").asText()).isEqualTo("Alice");

        // Un assureur hors organisation → 404 (existence masquée)
        ApiActor stranger = new ApiActor(port);
        String strangerId = stranger.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "stranger-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Stranger",
                                "createOrganization",
                                Map.of("name", "Autre club " + tag, "climbingType", "BOULDER")),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        assertThat(owner.post(
                                "/api/sessions/" + sessionId + "/ascents",
                                Map.of("routeId", routeId, "belayerUserId", strangerId),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void selfRequestIsRejectedAndFriendGraphIsIsolatedBetweenOrganizations() {
        // Demande à soi-même → 409
        assertThat(alice.post("/api/friends/requests", Map.of("addresseeId", aliceId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // Un membre d'un autre club
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
                        Map.of("name", "Club B " + tag, "climbingType", "BOULDER")),
                JsonNode.class);
        UUID ownerBId = UUID.fromString(
                ownerB.get("/api/auth/me", JsonNode.class).getBody().path("id").asText());

        // Alice ne peut pas envoyer de demande à un membre d'une autre organisation → 404
        assertThat(alice.post("/api/friends/requests", Map.of("addresseeId", ownerBId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Retrait d'un lien inexistant → 404
        assertThat(alice.delete("/api/friends/" + bobId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Le lien peut être retiré après acceptation
        alice.post("/api/friends/requests", Map.of("addresseeId", bobId), JsonNode.class);
        bob.post("/api/friends/" + aliceId + "/accept", null, JsonNode.class);
        assertThat(alice.delete("/api/friends/" + bobId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(alice.get("/api/friends", JsonNode.class).getBody()).isEmpty();
    }
}
