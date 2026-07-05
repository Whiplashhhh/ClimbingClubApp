package app.belay.message.dto;

import app.belay.message.Conversation;
import app.belay.message.ConversationType;
import java.time.Instant;
import java.util.UUID;

/**
 * Un fil présenté du point de vue de l'appelant. {@code title} est l'autre personne (DIRECT), le
 * nom du créneau (SLOT) ou « Tout le club » (GENERAL). {@code otherUserId} n'est renseigné que
 * pour les fils 1:1 (contexte d'un message privé côté front).
 */
public record ConversationResponse(
        UUID id,
        ConversationType type,
        String title,
        UUID otherUserId,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unread) {

    public static ConversationResponse direct(Conversation conversation, UUID selfId, String preview, long unread) {
        var other = conversation.directOther(selfId);
        return new ConversationResponse(
                conversation.getId(),
                ConversationType.DIRECT,
                other.getDisplayName(),
                other.getId(),
                preview,
                conversation.getLastMessageAt(),
                unread);
    }

    public static ConversationResponse group(Conversation conversation, String title, String preview, long unread) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                title,
                null,
                preview,
                conversation.getLastMessageAt(),
                unread);
    }
}
