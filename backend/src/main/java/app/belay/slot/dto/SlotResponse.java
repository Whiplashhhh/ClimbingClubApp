package app.belay.slot.dto;

import app.belay.slot.Slot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        String name,
        DayOfWeek dayOfWeek,

        @Schema(type = "string", format = "partial-time", example = "18:00")
        LocalTime startTime,

        int durationMinutes,
        UUID coachId,
        String coachDisplayName,
        List<SlotMemberResponse> members) {

    public static SlotResponse from(Slot slot, List<SlotMemberResponse> members) {
        return new SlotResponse(
                slot.getId(),
                slot.getName(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getDurationMinutes(),
                slot.getCoach().getId(),
                slot.getCoach().getDisplayName(),
                members);
    }
}
