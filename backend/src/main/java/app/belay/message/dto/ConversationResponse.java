package app.belay.message.dto;

import app.belay.message.Conversation;
import app.belay.user.AppUser;
import java.time.Instant;
import java.util.UUID;

/** Un fil présenté du point de vue de l'appelant : l'autre participant, aperçu, non-lus. */
public record ConversationResponse(
        UUID id,
        UUID otherUserId,
        String otherDisplayName,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unread) {

    public static ConversationResponse from(Conversation conversation, UUID selfId, String preview, long unread) {
        AppUser other = conversation.other(selfId);
        return new ConversationResponse(
                conversation.getId(),
                other.getId(),
                other.getDisplayName(),
                preview,
                conversation.getLastMessageAt(),
                unread);
    }
}
