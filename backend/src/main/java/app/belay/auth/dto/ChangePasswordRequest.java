package app.belay.auth.dto;

import app.belay.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword @Size(max = 72) String newPassword) {}
