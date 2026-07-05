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

/**
 * Chemin critique de la Phase 6B (messagerie privée) : un fil ne relie qu'un moniteur et l'un de
 * ses élèves (relation dérivée d'un créneau) ; échange de messages, non-lus et notification ;
 * isolation inter-organisations (anti-IDOR).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageIntegrationTest {

    @LocalServerPort
    private int port;

    private String tag;
    private ApiActor owner;
    private ApiActor coach;
    private ApiActor member;
    private ApiActor member2;
    private String coachId;
    private String memberId;
    private String member2Id;

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
        member2Id = joinAndApprove(member2, "member2-" + tag + "@club.fr", "Member Two", slug);

        // Le moniteur crée son créneau et rattache l'élève (member) — pas member2
        String slotId = coach.post(
                        "/api/slots",
                        Map.of("name", "Ados", "dayOfWeek", "THURSDAY", "startTime", "18:00", "durationMinutes", 90),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        coach.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class);
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
    void coachAndStudentExchangeMessagesWithUnreadAndNotification() {
        // L'élève démarre un fil avec son moniteur
        String conversationId = member.post("/api/conversations", Map.of("userId", coachId), JsonNode.class)
                .getBody()
                .path("id")
                .asText();

        // Il envoie un message
        assertThat(member.post(
                                "/api/conversations/" + conversationId + "/messages",
                                Map.of("body", "Bonjour coach !"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Le moniteur voit le fil avec l'aperçu et un non-lu
        JsonNode coachInbox =
                coach.get("/api/conversations", JsonNode.class).getBody().get(0);
        assertThat(coachInbox.path("otherDisplayName").asText()).isEqualTo("Member");
        assertThat(coachInbox.path("lastMessagePreview").asText()).isEqualTo("Bonjour coach !");
        assertThat(coachInbox.path("unread").asLong()).isEqualTo(1);

        // Il ouvre le fil → les messages sont visibles et marqués lus
        JsonNode thread = coach.get("/api/conversations/" + conversationId + "/messages", JsonNode.class)
                .getBody();
        assertThat(thread).hasSize(1);
        assertThat(thread.get(0).path("body").asText()).isEqualTo("Bonjour coach !");
        assertThat(coach.get("/api/conversations", JsonNode.class)
                        .getBody()
                        .get(0)
                        .path("unread")
                        .asLong())
                .isZero();

        // Il répond → l'élève a un non-lu ET une notification in-app
        coach.post(
                "/api/conversations/" + conversationId + "/messages", Map.of("body", "Salut, à jeudi"), JsonNode.class);
        assertThat(member.get("/api/conversations", JsonNode.class)
                        .getBody()
                        .get(0)
                        .path("unread")
                        .asLong())
                .isEqualTo(1);
        JsonNode memberNotifs = member.get("/api/notifications", JsonNode.class).getBody();
        assertThat(memberNotifs.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(memberNotifs.path("items").get(0).path("message").asText()).isEqualTo("Nouveau message de Coach");

        // Démarrer à nouveau le fil renvoie le même (pas de doublon)
        assertThat(coach.post("/api/conversations", Map.of("userId", memberId), JsonNode.class)
                        .getBody()
                        .path("id")
                        .asText())
                .isEqualTo(conversationId);
    }

    @Test
    void startingAConversationRequiresACoachingRelationship() {
        // member2 n'est rattaché à aucun créneau du moniteur → pas de fil possible
        assertThat(member2.post("/api/conversations", Map.of("userId", coachId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        // Deux simples membres entre eux → pas de relation moniteur/élève
        assertThat(member.post("/api/conversations", Map.of("userId", member2Id), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        // Soi-même → refusé
        assertThat(member.post("/api/conversations", Map.of("userId", memberId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        // Membre inexistant dans l'org → 404
        assertThat(member.post(
                                "/api/conversations",
                                Map.of("userId", UUID.randomUUID().toString()),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void conversationsAreIsolatedBetweenOrganizations() {
        String conversationId = member.post("/api/conversations", Map.of("userId", coachId), JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        member.post("/api/conversations/" + conversationId + "/messages", Map.of("body", "Interne"), JsonNode.class);

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

        // B ne voit aucun fil et ne peut ni lire ni écrire dans celui de A → 404 (existence masquée)
        assertThat(ownerB.get("/api/conversations", JsonNode.class).getBody()).isEmpty();
        assertThat(ownerB.get("/api/conversations/" + conversationId + "/messages", JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.post(
                                "/api/conversations/" + conversationId + "/messages",
                                Map.of("body", "Intrusion"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
