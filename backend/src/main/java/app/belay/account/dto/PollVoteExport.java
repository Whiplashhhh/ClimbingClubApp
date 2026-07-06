package app.belay.account.dto;

/** Vote de l'utilisateur à un sondage (export RGPD). */
public record PollVoteExport(String question, String option) {}
