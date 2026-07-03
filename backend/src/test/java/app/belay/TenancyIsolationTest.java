package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Test anti-IDOR inter-organisations (CLAUDE.md §10) : un membre — même OWNER — de l'org A ne
 * peut ni lire ni écrire les données de l'org B. Les ressources d'une autre org répondent 404
 * pour ne pas révéler leur existence.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenancyIsolationTest {

    @LocalServerPort
    private int port;

    @Test
    void ownerOfOrgACannotReadOrWriteOrgBData() {
        String tag = UUID.randomUUID().toString().substring(0, 8);

        // Org A : un owner seul
        ApiActor ownerA = new ApiActor(port);
        JsonNode a = ownerA.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "owner-a-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Owner A",
                                "createOrganization",
                                Map.of("name", "Org A " + tag, "climbingType", "BOULDER")),
                        JsonNode.class)
                .getBody();

        // Org B : un owner + un membre en attente
        ApiActor ownerB = new ApiActor(port);
        JsonNode b = ownerB.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "owner-b-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Owner B",
                                "createOrganization",
                                Map.of("name", "Org B " + tag, "climbingType", "ROPES")),
                        JsonNode.class)
                .getBody();
        String slugB = b.path("organization").path("slug").asText();
        String ownerBId = b.path("id").asText();

        ApiActor pendingB = new ApiActor(port);
        String pendingBId = pendingB.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "pending-b-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Pending B",
                                "joinSlug",
                                slugB),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();

        // Lecture : A ne voit que ses membres, jamais ceux de B
        ResponseEntity<JsonNode> membersOfA = ownerA.get("/api/members", JsonNode.class);
        assertThat(membersOfA.getBody().findValuesAsText("id"))
                .containsExactly(a.path("id").asText());
        assertThat(ownerA.get("/api/members/pending", JsonNode.class).getBody().findValuesAsText("id"))
                .doesNotContain(pendingBId);

        // Écriture : approuver ou changer le rôle d'un membre de B → 404 (pas 403 : existence masquée)
        assertThat(ownerA.post("/api/members/" + pendingBId + "/approve", null, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerA.patch("/api/members/" + ownerBId + "/role", Map.of("role", "MEMBER"), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Le membre de B n'a pas été altéré par les tentatives de A
        assertThat(ownerB.get("/api/members/pending", JsonNode.class).getBody().findValuesAsText("id"))
                .contains(pendingBId);
    }
}
