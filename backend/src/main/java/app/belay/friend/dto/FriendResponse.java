package app.belay.friend.dto;

import app.belay.user.AppUser;
import java.util.UUID;

/** Un ami (ou un demandeur), présenté par son identité minimale. */
public record FriendResponse(UUID id, String displayName) {

    public static FriendResponse from(AppUser user) {
        return new FriendResponse(user.getId(), user.getDisplayName());
    }
}
