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
 * Chemin critique de la messagerie (retours n°5) : fils 1:1 moniteur↔élève, groupes par créneau et
 * groupe général du club (auto-provisionnés, accès calculé), non-lus par participant, notification
 * réservée aux 1:1, et isolation inter-organisations (anti-IDOR).
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
    void directMessagingKeepsWorkingWithUnreadAndNotification() {
        String conversationId = member.post("/api/conversations", Map.of("userId", coachId), JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        member.post(
                "/api/conversations/" + conversationId + "/messages",
                Map.of("body", "Bonjour coach !"),
                JsonNode.class);

        JsonNode coachDirect = findByType(coach, "DIRECT");
        assertThat(coachDirect.path("title").asText()).isEqualTo("Member");
        assertThat(coachDirect.path("lastMessagePreview").asText()).isEqualTo("Bonjour coach !");
        assertThat(coachDirect.path("unread").asLong()).isEqualTo(1);

        // Ouvrir marque lu
        coach.get("/api/conversations/" + conversationId + "/messages", JsonNode.class);
        assertThat(findByType(coach, "DIRECT").path("unread").asLong()).isZero();

        // Réponse → l'élève a un non-lu ET une notification in-app (réservée aux 1:1)
        coach.post("/api/conversations/" + conversationId + "/messages", Map.of("body", "Salut"), JsonNode.class);
        assertThat(findByType(member, "DIRECT").path("unread").asLong()).isEqualTo(1);
        JsonNode notifs = member.get("/api/notifications", JsonNode.class).getBody();
        assertThat(notifs.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(notifs.path("items").get(0).path("message").asText()).isEqualTo("Nouveau message de Coach");
    }

    @Test
    void everyoneSeesTheGeneralGroupAndItDoesNotNotify() {
        // Le groupe général apparaît pour tous, même sans créneau (member2)
        assertThat(findByType(member2, "GENERAL").path("title").asText()).isEqualTo("Tout le club");
        String generalId = findByType(member, "GENERAL").path("id").asText();

        member.post("/api/conversations/" + generalId + "/messages", Map.of("body", "Salut le club"), JsonNode.class);

        // member2 voit le message en non-lu, sans notification (les groupes ne notifient pas)
        assertThat(findByType(member2, "GENERAL").path("unread").asLong()).isEqualTo(1);
        assertThat(member2.get("/api/notifications", JsonNode.class)
                        .getBody()
                        .path("unreadCount")
                        .asLong())
                .isZero();

        // member2 peut aussi écrire dans le groupe général (tout le monde écrit)
        assertThat(member2.post(
                                "/api/conversations/" + generalId + "/messages",
                                Map.of("body", "Coucou"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void slotGroupIsVisibleToParticipantsOnly() {
        // member (rattaché) voit le groupe du créneau ; member2 (non rattaché) non
        JsonNode slotGroup = findByTitle(member, "Ados");
        assertThat(slotGroup.path("type").asText()).isEqualTo("SLOT");
        String slotGroupId = slotGroup.path("id").asText();
        assertThat(findByTitleOrNull(member2, "Ados")).isNull();

        // Le moniteur poste dans le groupe → l'élève a un non-lu
        coach.post("/api/conversations/" + slotGroupId + "/messages", Map.of("body", "RDV jeudi"), JsonNode.class);
        assertThat(findByTitle(member, "Ados").path("unread").asLong()).isEqualTo(1);

        // member2 ne peut ni lire ni écrire dans ce groupe → 404 (existence masquée)
        assertThat(member2.get("/api/conversations/" + slotGroupId + "/messages", JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(member2.post(
                                "/api/conversations/" + slotGroupId + "/messages",
                                Map.of("body", "Intrus"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void conversationsAreIsolatedBetweenOrganizations() {
        String generalId = findByType(member, "GENERAL").path("id").asText();
        member.post("/api/conversations/" + generalId + "/messages", Map.of("body", "Interne"), JsonNode.class);

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

        // B a SON propre groupe général, jamais celui de A ; il ne peut lire/écrire dans le fil de A
        JsonNode bGeneral = findByType(ownerB, "GENERAL");
        assertThat(bGeneral.path("id").asText()).isNotEqualTo(generalId);
        assertThat(ownerB.get("/api/conversations/" + generalId + "/messages", JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.post(
                                "/api/conversations/" + generalId + "/messages",
                                Map.of("body", "Intrusion"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private JsonNode conversations(ApiActor actor) {
        return actor.get("/api/conversations", JsonNode.class).getBody();
    }

    private JsonNode findByType(ApiActor actor, String type) {
        for (JsonNode c : conversations(actor)) {
            if (c.path("type").asText().equals(type)) {
                return c;
            }
        }
        throw new AssertionError("No conversation of type " + type);
    }

    private JsonNode findByTitle(ApiActor actor, String title) {
        JsonNode found = findByTitleOrNull(actor, title);
        if (found == null) {
            throw new AssertionError("No conversation titled " + title);
        }
        return found;
    }

    private JsonNode findByTitleOrNull(ApiActor actor, String title) {
        for (JsonNode c : conversations(actor)) {
            if (c.path("title").asText().equals(title)) {
                return c;
            }
        }
        return null;
    }
}
