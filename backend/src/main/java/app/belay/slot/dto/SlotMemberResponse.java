package app.belay.slot.dto;

import app.belay.user.AppUser;
import java.util.UUID;

public record SlotMemberResponse(UUID id, String displayName) {

    public static SlotMemberResponse from(AppUser user) {
        return new SlotMemberResponse(user.getId(), user.getDisplayName());
    }
}
