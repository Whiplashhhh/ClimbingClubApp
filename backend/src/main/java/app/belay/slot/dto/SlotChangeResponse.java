package app.belay.slot.dto;

import app.belay.slot.SlotChange;
import app.belay.slot.SlotChangeAction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SlotChangeResponse(
        UUID id,
        LocalDate date,
        SlotChangeAction action,

        @Schema(type = "string", format = "partial-time", example = "19:00")
        LocalTime newStartTime,

        String note) {

    public static SlotChangeResponse from(SlotChange change) {
        return new SlotChangeResponse(
                change.getId(), change.getDate(), change.getAction(), change.getNewStartTime(), change.getNote());
    }
}
