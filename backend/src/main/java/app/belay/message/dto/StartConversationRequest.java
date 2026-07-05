package app.belay.message.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Démarre (ou récupère) un fil avec l'autre participant — un moniteur ou l'un de ses élèves. */
public record StartConversationRequest(@NotNull UUID userId) {}
