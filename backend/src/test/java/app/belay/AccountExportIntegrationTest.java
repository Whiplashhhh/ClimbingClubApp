package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Export RGPD : le document réunit les données personnelles de l'appelant (profil, séances, votes,
 * messages) et reste scopé à lui — pas de fuite du profil d'un autre membre.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountExportIntegrationTest {

    @LocalServerPort
    private int port;

    private ApiActor owner;
    private ApiActor member;
    private String memberEmail;

    @BeforeEach
    void setUp() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
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
        memberEmail = "member-" + tag + "@club.fr";
        String memberId = member.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                memberEmail,
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
    }

    @Test
    void exportGathersTheCallersOwnData() {
        // Un sondage de l'owner, voté par le membre
        JsonNode poll = owner.post(
                        "/api/polls",
                        Map.of("audience", "ORG", "question", "Sortie où ?", "options", List.of("Bleau", "Ceüse")),
                        JsonNode.class)
                .getBody();
        String pollId = poll.path("id").asText();
        String option = poll.path("options").get(0).path("id").asText();
        member.post("/api/polls/" + pollId + "/vote", Map.of("optionId", option), JsonNode.class);

        // Une séance du membre
        member.post("/api/sessions", Map.of("visibility", "PRIVATE"), JsonNode.class);

        // Un message du membre dans le groupe général
        String generalId = null;
        for (JsonNode c : member.get("/api/conversations", JsonNode.class).getBody()) {
            if (c.path("type").asText().equals("GENERAL")) {
                generalId = c.path("id").asText();
            }
        }
        member.post("/api/conversations/" + generalId + "/messages", Map.of("body", "Coucou export"), JsonNode.class);

        JsonNode export = member.get("/api/account/export", JsonNode.class).getBody();

        // Le profil exporté est celui du membre (pas celui de l'owner)
        assertThat(export.path("profile").path("email").asText()).isEqualTo(memberEmail);
        // Le vote, la séance et le message de l'appelant sont présents
        assertThat(export.path("pollVotes").get(0).path("question").asText()).isEqualTo("Sortie où ?");
        assertThat(export.path("pollVotes").get(0).path("option").asText()).isEqualTo("Bleau");
        assertThat(export.path("sessions")).isNotEmpty();
        assertThat(export.path("messages").findValuesAsText("body")).contains("Coucou export");

        // L'export de l'owner ne contient pas le vote/le message du membre (isolation par appelant)
        JsonNode ownerExport = owner.get("/api/account/export", JsonNode.class).getBody();
        assertThat(ownerExport.path("profile").path("email").asText()).isNotEqualTo(memberEmail);
        assertThat(ownerExport.path("messages").findValuesAsText("body")).doesNotContain("Coucou export");
    }
}
