package app.belay.message.dto;

import app.belay.organization.Organization;

/**
 * Réglages de messagerie du club. {@code generalChatUnlimited} vrai = pas de limite ; sinon
 * {@code generalChatRateLimit} messages autorisés par membre et par {@code generalChatWindowSeconds}.
 */
public record MessagingSettingsResponse(
        boolean generalChatUnlimited, Integer generalChatRateLimit, Integer generalChatWindowSeconds) {

    public static MessagingSettingsResponse from(Organization org) {
        return new MessagingSettingsResponse(
                org.getGeneralChatRateLimit() == null,
                org.getGeneralChatRateLimit(),
                org.getGeneralChatWindowSeconds());
    }
}
