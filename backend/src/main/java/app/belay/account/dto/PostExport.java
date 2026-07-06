package app.belay.account.dto;

import java.time.Instant;
import java.util.UUID;

/** Publication rédigée par l'utilisateur (export RGPD). */
public record PostExport(UUID id, String title, String body, Instant createdAt) {}
