package app.belay.auth.dto;

import app.belay.common.validation.StrongPassword;
import app.belay.organization.ClimbingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Inscription : exactement un des deux champs {@code createOrganization} / {@code joinSlug} doit
 * être fourni (créer un club ou rejoindre un club existant).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @StrongPassword @Size(max = 72) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Valid CreateOrganization createOrganization,

        @Schema(description = "Slug of the organization to join as a pending member") @Size(max = 140)
        String joinSlug) {

    public record CreateOrganization(
            @NotBlank @Size(max = 120) String name, @NotNull ClimbingType climbingType) {}

    public boolean isCreateMode() {
        return createOrganization != null;
    }

    public boolean isJoinMode() {
        return joinSlug != null && !joinSlug.isBlank();
    }
}
