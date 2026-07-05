package app.belay.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAscentRequest(
        @NotNull UUID routeId,

        @Min(1) @Max(5) @Schema(description = "Appreciation 1..5")
        Short rating,

        @PositiveOrZero @Schema(description = "Highest hold reached")
        Short topHold,

        @PositiveOrZero @Schema(description = "Duration in seconds (optional)")
        Integer durationSeconds,

        @Size(max = 120) @Schema(description = "Free-text belayer name (a friend selector comes with the friend graph)")
        String belayerName) {}
