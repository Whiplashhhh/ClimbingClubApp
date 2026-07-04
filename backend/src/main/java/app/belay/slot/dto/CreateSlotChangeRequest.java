package app.belay.slot.dto;

import app.belay.slot.SlotChangeAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateSlotChangeRequest(
        @NotNull @Schema(description = "Session date — must fall on the slot's weekday")
        LocalDate date,

        @NotNull SlotChangeAction action,

        @Schema(type = "string", format = "partial-time", example = "19:00", description = "Required when MOVED")
        LocalTime newStartTime,

        @Size(max = 500) String note) {}
