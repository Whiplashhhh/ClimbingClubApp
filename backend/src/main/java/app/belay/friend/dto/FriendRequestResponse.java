package app.belay.friend.dto;

import app.belay.friend.Friendship;

/** Demande d'ami en attente, présentée avec le demandeur. */
public record FriendRequestResponse(FriendResponse requester) {

    public static FriendRequestResponse from(Friendship friendship) {
        return new FriendRequestResponse(FriendResponse.from(friendship.getRequester()));
    }
}
