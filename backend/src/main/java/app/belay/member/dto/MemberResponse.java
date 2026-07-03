package app.belay.member.dto;

import app.belay.user.AppUser;
import app.belay.user.Role;
import java.util.UUID;

/** Vue « membre » minimale (pas d'email : minimisation des données). */
public record MemberResponse(UUID id, String displayName, Role role) {

    public static MemberResponse from(AppUser user) {
        return new MemberResponse(user.getId(), user.getDisplayName(), user.getRole());
    }
}
