package app.belay.account.dto;

import java.time.Instant;
import java.util.UUID;

/** Message rédigé par l'utilisateur (export RGPD). */
public record MessageExport(UUID conversationId, String body, Instant createdAt) {}
