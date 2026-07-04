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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Chemin critique de la Phase 3 (créneaux & groupes) : un post COACH_STUDENTS devient visible
 * pour les élèves via leur rattachement au créneau du moniteur (A-005) ; matrice de permissions
 * de gestion des créneaux ; isolation inter-organisations (anti-IDOR).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SlotIntegrationTest {

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
    void coachStudentsPostsBecomeVisibleThroughSlotMembership() {
        // Le moniteur crée son créneau et publie pour ses élèves
        String slotId =
                createSlot(coach, "Ados jeudi", null).getBody().path("id").asText();
        coach.post(
                "/api/posts",
                Map.of("type", "INFO", "audience", "COACH_STUDENTS", "title", "Sortie falaise samedi"),
                JsonNode.class);

        // Pas encore élève → invisible
        assertThat(feedTitles(member)).doesNotContain("Sortie falaise samedi");

        // Le moniteur rattache l'élève à son créneau → le post apparaît dans SON fil, pas ailleurs
        ResponseEntity<JsonNode> added =
                coach.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class);
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(feedTitles(member)).contains("Sortie falaise samedi");
        assertThat(feedTitles(member2)).doesNotContain("Sortie falaise samedi");

        // Le planning liste le créneau avec son groupe, visible par tous les membres actifs
        JsonNode slots = member2.get("/api/slots", JsonNode.class).getBody();
        assertThat(slots.get(0).path("name").asText()).isEqualTo("Ados jeudi");
        assertThat(slots.get(0).path("coachDisplayName").asText()).isEqualTo("Coach");
        assertThat(slots.get(0).path("members").findValuesAsText("displayName")).containsExactly("Member");

        // Retiré du créneau → le post disparaît du fil
        assertThat(coach.delete("/api/slots/" + slotId + "/members/" + memberId, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(feedTitles(member)).doesNotContain("Sortie falaise samedi");
    }

    @Test
    void slotManagementFollowsThePermissionMatrix() {
        // Un simple membre ne crée pas de créneau
        assertThat(createSlot(member, "Interdit", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Un moniteur ne crée pas de créneau pour quelqu'un d'autre
        assertThat(createSlot(coach, "Pour un autre", memberId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Un admin peut créer pour un moniteur, mais pas pour un simple membre (inéligible)
        assertThat(createSlot(owner, "Cours du coach", coachId).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createSlot(owner, "Coach invalide", memberId).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        String slotId =
                createSlot(coach, "Perf vendredi", null).getBody().path("id").asText();

        // Rattachements : un membre encore PENDING est refusé, un doublon aussi
        String slug = owner.get("/api/auth/me", JsonNode.class)
                .getBody()
                .path("organization")
                .path("slug")
                .asText();
        ApiActor pending = new ApiActor(port);
        String pendingId = pending.post(
                        "/api/auth/register",
                        Map.of(
                                "email",
                                "pending-" + tag + "@club.fr",
                                "password",
                                "s3cure-password",
                                "displayName",
                                "Pending",
                                "joinSlug",
                                slug),
                        JsonNode.class)
                .getBody()
                .path("id")
                .asText();
        assertThat(coach.post("/api/slots/" + slotId + "/members", Map.of("userId", pendingId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        coach.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class);
        assertThat(coach.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // L'admin peut modifier le créneau du moniteur ; un simple membre non (403 avant tenancy)
        assertThat(owner.post("/api/slots/" + slotId + "/members", Map.of("userId", coachId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(member.delete("/api/slots/" + slotId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Suppression par le moniteur : le créneau disparaît du planning
        assertThat(coach.delete("/api/slots/" + slotId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(owner.get("/api/slots", JsonNode.class).getBody().findValuesAsText("name"))
                .doesNotContain("Perf vendredi");
    }

    @Test
    void slotsAreIsolatedBetweenOrganizations() {
        String slotId =
                createSlot(coach, "Interne " + tag, null).getBody().path("id").asText();

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

        // Lecture : le planning de B ne contient jamais les créneaux de A
        assertThat(ownerB.get("/api/slots", JsonNode.class).getBody()).isEmpty();

        // Écriture : gestion inter-org → 404 (existence masquée), le créneau de A est intact
        assertThat(ownerB.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownerB.delete("/api/slots/" + slotId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(member.get("/api/slots", JsonNode.class).getBody().findValuesAsText("name"))
                .contains("Interne " + tag);
    }

    @Test
    void cancellingASessionNotifiesTheGroupInApp() {
        String slotId =
                createSlot(coach, "Ados jeudi", null).getBody().path("id").asText();
        coach.post("/api/slots/" + slotId + "/members", Map.of("userId", memberId), JsonNode.class);

        java.time.LocalDate nextThursday =
                java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.THURSDAY));

        // Garde-fous : mauvais jour de semaine, heure manquante pour un décalage
        assertThat(coach.post(
                                "/api/slots/" + slotId + "/changes",
                                Map.of("date", nextThursday.plusDays(1).toString(), "action", "CANCELLED"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(coach.post(
                                "/api/slots/" + slotId + "/changes",
                                Map.of("date", nextThursday.toString(), "action", "MOVED"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Annulation → l'élève du groupe est notifié, pas les autres, pas l'auteur
        ResponseEntity<JsonNode> cancelled = coach.post(
                "/api/slots/" + slotId + "/changes",
                Map.of("date", nextThursday.toString(), "action", "CANCELLED", "note", "Coach malade"),
                JsonNode.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cancelled.getBody().path("changes").get(0).path("action").asText())
                .isEqualTo("CANCELLED");

        JsonNode memberNotifications =
                member.get("/api/notifications", JsonNode.class).getBody();
        assertThat(memberNotifications.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(memberNotifications.path("items").get(0).path("message").asText())
                .contains("Ados jeudi")
                .contains("annulée")
                .contains("Coach malade");

        // L'annulation est aussi publiée dans le fil de l'élève (post CANCELLATION au nom du
        // moniteur), pas dans celui des membres hors groupe
        JsonNode memberFeedItem =
                member.get("/api/feed", JsonNode.class).getBody().path("items").get(0);
        assertThat(memberFeedItem.path("type").asText()).isEqualTo("CANCELLATION");
        assertThat(memberFeedItem.path("title").asText()).contains("Ados jeudi").contains("annulée");
        assertThat(memberFeedItem.path("body").asText()).isEqualTo("Coach malade");
        assertThat(feedTitles(member2)).noneMatch(title -> title.contains("annulée"));
        assertThat(member2.get("/api/notifications", JsonNode.class)
                        .getBody()
                        .path("unreadCount")
                        .asLong())
                .isZero();
        assertThat(coach.get("/api/notifications", JsonNode.class)
                        .getBody()
                        .path("unreadCount")
                        .asLong())
                .isZero();

        // La même séance ne peut pas être modifiée deux fois
        assertThat(coach.post(
                                "/api/slots/" + slotId + "/changes",
                                Map.of("date", nextThursday.toString(), "action", "CANCELLED"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // Tout marquer lu
        assertThat(member.post("/api/notifications/read-all", null, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(member.get("/api/notifications", JsonNode.class)
                        .getBody()
                        .path("unreadCount")
                        .asLong())
                .isZero();

        // Rétablir la séance : le post automatique disparaît du fil ; puis décaler une autre
        // séance → nouvelle notification avec l'heure
        String changeId = cancelled.getBody().path("changes").get(0).path("id").asText();
        assertThat(coach.delete("/api/slots/" + slotId + "/changes/" + changeId, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(feedTitles(member)).noneMatch(title -> title.contains("annulée"));
        coach.post(
                "/api/slots/" + slotId + "/changes",
                Map.of(
                        "date", nextThursday.plusWeeks(1).toString(),
                        "action", "MOVED",
                        "newStartTime", "19:00"),
                JsonNode.class);
        JsonNode afterMove = member.get("/api/notifications", JsonNode.class).getBody();
        assertThat(afterMove.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(afterMove.path("items").get(0).path("message").asText()).contains("décalée à 19:00");
    }

    private ResponseEntity<JsonNode> createSlot(ApiActor actor, String name, String coachId) {
        Map<String, Object> body = coachId == null
                ? Map.of("name", name, "dayOfWeek", "THURSDAY", "startTime", "18:00", "durationMinutes", 90)
                : Map.of(
                        "name",
                        name,
                        "dayOfWeek",
                        "THURSDAY",
                        "startTime",
                        "18:00",
                        "durationMinutes",
                        90,
                        "coachId",
                        coachId);
        return actor.post("/api/slots", body, JsonNode.class);
    }

    private List<String> feedTitles(ApiActor actor) {
        return actor.get("/api/feed", JsonNode.class).getBody().path("items").findValuesAsText("title");
    }
}
