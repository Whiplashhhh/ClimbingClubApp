package app.belay.slot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record CreateSlotRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull DayOfWeek dayOfWeek,

        @NotNull @Schema(type = "string", format = "partial-time", example = "18:00")
        LocalTime startTime,

        @NotNull @Min(15) @Max(600) Integer durationMinutes,

        @Schema(description = "Coach of the slot — defaults to the caller. Admins may target any eligible member.")
        UUID coachId) {}
