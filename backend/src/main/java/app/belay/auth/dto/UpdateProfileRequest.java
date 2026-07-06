package app.belay.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mise à jour du profil, confirmée par le mot de passe actuel. {@code displayName} et {@code email}
 * sont optionnels (on change l'un, l'autre ou les deux).
 */
public record UpdateProfileRequest(
        @Size(max = 120) String displayName,
        @Email @Size(max = 255) String email,
        @NotBlank String currentPassword) {}
