package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Chemin critique de la Phase 6 (sondages) : création selon la matrice d'audience du fil, vote à
 * choix unique (remplaçable) avec décomptes, fermeture, visibilité COACH_STUDENTS via le créneau,
 * et isolation inter-organisations (anti-IDOR).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PollIntegrationTest {

    @LocalServerPort
    private int port;

    private String tag;
    private ApiActor owner;
    private ApiActor coach;
    private ApiActor member;
    private ApiActor member2;
    private String coachId;
    private String memberId;

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
        coach = new ApiActor(port);
        coachId = joinAndApprove(coach, "coach-" + tag + "@club.fr", "Coach", slug);
        owner.patch("/api/members/" + coachId + "/role", Map.of("role", "COACH"), JsonNode.class);
        member = new ApiActor(port);
        memberId = joinAndApprove(member, "member-" + tag + "@club.fr", "Member", slug);
        member2 = new ApiActor(port);
        joinAndApprove(member2, "member2-" + tag + "@club.fr", "Member Two", slug);
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
    void orgPollCanBeVotedAndRevoted() {
        JsonNode poll = createPoll(
                        owner, "ORG", "Sortie où ce week-end ?", List.of("Fontainebleau", "Le Saussois"), null)
                .getBody();
        String pollId = poll.path("id").asText();
        String fontainebleau = poll.path("options").get(0).path("id").asText();
        String saussois = poll.path("options").get(1).path("id").asText();

        // Le membre voit le sondage ORG, sans choix ni voix
        JsonNode listed = member.get("/api/polls", JsonNode.class).getBody().get(0);
        assertThat(listed.path("question").asText()).isEqualTo("Sortie où ce week-end ?");
        // Pas encore voté : le champ est absent (inclusion non-null) ou null
        assertThat(listed.hasNonNull("myOptionId")).isFalse();
        assertThat(listed.path("totalVotes").asLong()).isZero();

        // Il vote Fontainebleau
        JsonNode afterVote = vote(member, pollId, fontainebleau).getBody();
        assertThat(afterVote.path("myOptionId").asText()).isEqualTo(fontainebleau);
        assertThat(afterVote.path("totalVotes").asLong()).isEqualTo(1);
        assertThat(votesFor(afterVote, fontainebleau)).isEqualTo(1);

        // Il change d'avis : Le Saussois — toujours une seule voix, déplacée
        JsonNode changed = vote(member, pollId, saussois).getBody();
        assertThat(changed.path("myOptionId").asText()).isEqualTo(saussois);
        assertThat(changed.path("totalVotes").asLong()).isEqualTo(1);
        assertThat(votesFor(changed, fontainebleau)).isZero();
        assertThat(votesFor(changed, saussois)).isEqualTo(1);

        // L'owner vote aussi → deux voix au total
        assertThat(vote(owner, pollId, fontainebleau)
                        .getBody()
                        .path("totalVotes")
                        .asLong())
                .isEqualTo(2);

        // Validation : moins de deux options → 400
        assertThat(createPoll(owner, "ORG", "Trop court", List.of("Seule option"), null)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Un membre peut supprimer... non : seulement l'auteur ou un admin
        assertThat(member.delete("/api/polls/" + pollId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(owner.delete("/api/polls/" + pollId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(member.get("/api/polls", JsonNode.class).getBody()).isEmpty();
    }

    @Test
    void coachStudentsPollIsVisibleThroughSlotMembershipOnly() {
        // Le moniteur crée son propre créneau : le groupe est dérivé de SES rattachements, et le
        // prédicat de visibilité COACH_STUDENTS relie slot.coach à l'auteur du sondage (le coach).
        String slotId = coach.post(
                        "/api/slots",
                        Map.of("name", "Ados", "dayOfWeek", "THURSDAY", "startTime", "18:00", "durationMinutes", 90),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        // Un moniteur ne peut publier qu'en COACH_STUDENTS, pas en ORG
        assertThat(createPoll(coach, "ORG", "Interdit", List.of("A", "B"), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // Un simple membre ne crée aucun sondage
        assertThat(createPoll(member, "COACH_STUDENTS", "Interdit aussi", List.of("A", "B"), null)
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        createPoll(coach, "COACH_STUDENTS", "Prochain créneau : plutôt ?", List.of("Bloc", "Voie"), null);
        assertThat(questions(member)).doesNotContain("Prochain créneau : plutôt ?");

        // Le moniteur rattache l'élève → le sondage apparaît pour lui, pas pour member2
        owner.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class);
        assertThat(questions(member)).contains("Prochain créneau : plutôt ?");
        assertThat(questions(member2)).doesNotContain("Prochain créneau : plutôt ?");
    }

    @Test
    void closedPollAndUnknownOptionAreRejected() {
        JsonNode poll = createPoll(
                        owner,
                        "ORG",
                        "Déjà fermé",
                        List.of("Oui", "Non"),
                        Instant.now().minus(1, ChronoUnit.HOURS))
                .getBody();
        String pollId = poll.path("id").asText();
        String option = poll.path("options").get(0).path("id").asText();

        assertThat(poll.path("closed").asBoolean()).isTrue();
        // Voter sur un sondage fermé → 409
        assertThat(vote(member, pollId, option).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Sur un sondage ouvert, une option inconnue → 404
        String openPollId = createPoll(owner, "ORG", "Ouvert", List.of("X", "Y"), null)
                .getBody()
                .path("id")
                .asText();
        assertThat(vote(member, openPollId, UUID.randomUUID().toString()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void pollsAreIsolatedBetweenOrganizations() {
        JsonNode poll = createPoll(owner, "ORG", "Interne " + tag, List.of("A", "B"), null)
                .getBody();
        String pollId = poll.path("id").asText();
        String option = poll.path("options").get(0).path("id").asText();

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

        // Lecture : B ne voit jamais le sondage de A
        assertThat(ownerB.get("/api/polls", JsonNode.class).getBody()).isEmpty();
        // Vote et suppression inter-org → 404 (existence masquée)
        assertThat(vote(ownerB, pollId, option).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.delete("/api/polls/" + pollId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        // Le sondage de A est intact
        assertThat(questions(member)).contains("Interne " + tag);
    }

    private ResponseEntity<JsonNode> createPoll(
            ApiActor actor, String audience, String question, List<String> options, Instant closesAt) {
        Map<String, Object> body = closesAt == null
                ? Map.of("audience", audience, "question", question, "options", options)
                : Map.of(
                        "audience",
                        audience,
                        "question",
                        question,
                        "options",
                        options,
                        "closesAt",
                        closesAt.toString());
        return actor.post("/api/polls", body, JsonNode.class);
    }

    private ResponseEntity<JsonNode> vote(ApiActor actor, String pollId, String optionId) {
        return actor.post("/api/polls/" + pollId + "/vote", Map.of("optionId", optionId), JsonNode.class);
    }

    private List<String> questions(ApiActor actor) {
        return actor.get("/api/polls", JsonNode.class).getBody().findValuesAsText("question");
    }

    private long votesFor(JsonNode poll, String optionId) {
        for (JsonNode option : poll.path("options")) {
            if (option.path("id").asText().equals(optionId)) {
                return option.path("votes").asLong();
            }
        }
        return -1;
    }
}
