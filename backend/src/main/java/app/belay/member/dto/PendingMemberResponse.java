package app.belay.member.dto;

import app.belay.user.AppUser;
import java.util.UUID;

/** Vue admin d'un membre en attente : l'email est nécessaire pour identifier la demande. */
public record PendingMemberResponse(UUID id, String displayName, String email) {

    public static PendingMemberResponse from(AppUser user) {
        return new PendingMemberResponse(user.getId(), user.getDisplayName(), user.getEmail());
    }
}
