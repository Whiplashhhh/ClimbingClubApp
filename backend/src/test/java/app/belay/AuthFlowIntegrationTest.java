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

/** Chemin critique Phase 1 : inscription, connexion, validation d'un membre, rôles, logout. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @LocalServerPort
    private int port;

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void fullMembershipLifecycle() {
        String tag = unique();
        ApiActor owner = new ApiActor(port);
        ApiActor member = new ApiActor(port);

        // 1. Le fondateur crée son club : OWNER, ACTIVE, session établie
        ResponseEntity<JsonNode> created = owner.post(
                "/api/auth/register",
                Map.of(
                        "email",
                        "owner-" + tag + "@club.fr",
                        "password",
                        "s3cure-password",
                        "displayName",
                        "Prési " + tag,
                        "createOrganization",
                        Map.of("name", "Roc Altitude " + tag, "climbingType", "BOTH")),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().path("role").asText()).isEqualTo("OWNER");
        assertThat(created.getBody().path("status").asText()).isEqualTo("ACTIVE");
        String slug = created.getBody().path("organization").path("slug").asText();
        assertThat(slug).isNotBlank();
        assertThat(owner.get("/api/auth/me", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Un grimpeur rejoint le club : PENDING, aucun accès aux données de l'org
        ResponseEntity<JsonNode> joined = member.post(
                "/api/auth/register",
                Map.of(
                        "email",
                        "member-" + tag + "@club.fr",
                        "password",
                        "s3cure-password",
                        "displayName",
                        "Grimpeur " + tag,
                        "joinSlug",
                        slug),
                JsonNode.class);
        assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(joined.getBody().path("status").asText()).isEqualTo("PENDING");
        String memberId = joined.getBody().path("id").asText();
        assertThat(member.get("/api/members", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 3. L'admin voit la demande et l'approuve
        ResponseEntity<JsonNode> pending = owner.get("/api/members/pending", JsonNode.class);
        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pending.getBody().findValuesAsText("id")).contains(memberId);
        ResponseEntity<JsonNode> approved = owner.post("/api/members/" + memberId + "/approve", null, JsonNode.class);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Le membre approuvé accède aux membres de son org ; le statut est resynchronisé
        //    sans nouvelle connexion (AccountRefreshFilter)
        ResponseEntity<JsonNode> members = member.get("/api/members", JsonNode.class);
        assertThat(members.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(members.getBody().size()).isEqualTo(2);

        // 5. Un simple membre ne peut pas administrer les rôles
        String ownerId = created.getBody().path("id").asText();
        assertThat(member.patch("/api/members/" + ownerId + "/role", Map.of("role", "COACH"), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // 6. L'admin promeut le membre moniteur
        assertThat(owner.patch("/api/members/" + memberId + "/role", Map.of("role", "COACH"), JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // 7. Déconnexion : session invalidée côté serveur
        assertThat(member.post("/api/auth/logout", null, Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(member.get("/api/auth/me", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginRejectsBadCredentialsWithGenericError() {
        String tag = unique();
        ApiActor actor = new ApiActor(port);
        actor.post(
                "/api/auth/register",
                Map.of(
                        "email",
                        "bad-" + tag + "@club.fr",
                        "password",
                        "s3cure-password",
                        "displayName",
                        "X",
                        "createOrganization",
                        Map.of("name", "Club " + tag, "climbingType", "BOULDER")),
                JsonNode.class);

        ResponseEntity<JsonNode> login = actor.post(
                "/api/auth/login",
                Map.of("email", "bad-" + tag + "@club.fr", "password", "wrong-password"),
                JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        String tag = unique();
        ApiActor actor = new ApiActor(port);
        Map<String, Object> request = Map.of(
                "email",
                "dup-" + tag + "@club.fr",
                "password",
                "s3cure-password",
                "displayName",
                "X",
                "createOrganization",
                Map.of("name", "Club " + tag, "climbingType", "ROPES"));
        assertThat(actor.post("/api/auth/register", request, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(new ApiActor(port)
                        .post("/api/auth/register", request, JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changePasswordUpdatesLoginCredentials() {
        String tag = unique();
        ApiActor actor = new ApiActor(port);
        actor.post(
                "/api/auth/register",
                Map.of(
                        "email",
                        "pwd-" + tag + "@club.fr",
                        "password",
                        "s3cure-password",
                        "displayName",
                        "X",
                        "createOrganization",
                        Map.of("name", "Club " + tag, "climbingType", "BOULDER")),
                JsonNode.class);

        // Mauvais mot de passe actuel → 400
        assertThat(actor.post(
                                "/api/auth/change-password",
                                Map.of("currentPassword", "wrong", "newPassword", "new-s3cure-pass"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Nouveau mot de passe trop court → 400
        assertThat(actor.post(
                                "/api/auth/change-password",
                                Map.of("currentPassword", "s3cure-password", "newPassword", "short"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Assez long mais une seule classe de caractères → 400 (politique de robustesse)
        assertThat(actor.post(
                                "/api/auth/change-password",
                                Map.of("currentPassword", "s3cure-password", "newPassword", "onlylowercase"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Changement valide → 204
        assertThat(actor.post(
                                "/api/auth/change-password",
                                Map.of("currentPassword", "s3cure-password", "newPassword", "new-s3cure-pass"),
                                Void.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        // L'ancien mot de passe ne fonctionne plus ; le nouveau oui
        ApiActor fresh = new ApiActor(port);
        assertThat(fresh.post(
                                "/api/auth/login",
                                Map.of("email", "pwd-" + tag + "@club.fr", "password", "s3cure-password"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new ApiActor(port)
                        .post(
                                "/api/auth/login",
                                Map.of("email", "pwd-" + tag + "@club.fr", "password", "new-s3cure-pass"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void mutationsWithoutCsrfTokenAreRejected() {
        // Anonyme : la protection CSRF déclenche le point d'entrée d'authentification → 401
        ApiActor anonymous = new ApiActor(port);
        assertThat(anonymous
                        .postWithoutCsrf(
                                "/api/auth/login",
                                Map.of("email", "x@y.fr", "password", "whatever-123"),
                                JsonNode.class)
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // Authentifié : mutation sans token → 403, avec token → passe la barrière CSRF
        String tag = unique();
        ApiActor actor = new ApiActor(port);
        actor.post(
                "/api/auth/register",
                Map.of(
                        "email",
                        "csrf-" + tag + "@club.fr",
                        "password",
                        "s3cure-password",
                        "displayName",
                        "X",
                        "createOrganization",
                        Map.of("name", "Club " + tag, "climbingType", "BOULDER")),
                JsonNode.class);
        assertThat(actor.postWithoutCsrf("/api/auth/logout", null, Void.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(actor.post("/api/auth/logout", null, Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }
}
