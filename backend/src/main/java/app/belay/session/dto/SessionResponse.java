package app.belay.session.dto;

import app.belay.session.ClimbingSession;
import app.belay.session.SessionVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        Instant startedAt,
        String note,
        SessionVisibility visibility,
        List<AscentResponse> ascents) {

    public static SessionResponse from(ClimbingSession session, List<AscentResponse> ascents) {
        return new SessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getUser().getDisplayName(),
                session.getStartedAt(),
                session.getNote(),
                session.getVisibility(),
                ascents);
    }
}
