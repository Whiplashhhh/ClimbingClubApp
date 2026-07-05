package app.belay.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Met à jour la limite du groupe général. Les deux champs à {@code null} = illimité ; sinon les
 * deux doivent être positifs (N messages par fenêtre de {@code windowSeconds} secondes).
 */
public record UpdateMessagingSettingsRequest(
        @Positive @Schema(description = "Max messages per member per window; null = unlimited")
        Integer generalChatRateLimit,

        @Positive @Schema(description = "Window length in seconds; null = unlimited")
        Integer generalChatWindowSeconds) {}
