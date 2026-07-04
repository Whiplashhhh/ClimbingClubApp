package app.belay.notification.dto;

import app.belay.notification.Notification;
import app.belay.notification.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, String message, Instant readAt, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
