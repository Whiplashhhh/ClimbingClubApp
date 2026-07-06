package app.belay.account.dto;

import app.belay.auth.dto.MeResponse;
import app.belay.friend.dto.FriendResponse;
import app.belay.notification.dto.NotificationResponse;
import app.belay.session.dto.SessionResponse;
import java.util.List;

/** Export RGPD : toutes les données personnelles de l'appelant, réunies en un document. */
public record AccountExportResponse(
        MeResponse profile,
        List<SessionResponse> sessions,
        List<FriendResponse> friends,
        List<PostExport> posts,
        List<PollVoteExport> pollVotes,
        List<MessageExport> messages,
        List<NotificationResponse> notifications) {}
