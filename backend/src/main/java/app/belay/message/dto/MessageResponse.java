package app.belay.message.dto;

import app.belay.message.Message;
import java.time.Instant;
import java.util.UUID;

/** Un message d'un fil, avec son auteur. Le front compare {@code senderId} pour aligner l'affichage. */
public record MessageResponse(UUID id, UUID senderId, String senderDisplayName, String body, Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getDisplayName(),
                message.getBody(),
                message.getCreatedAt());
    }
}
