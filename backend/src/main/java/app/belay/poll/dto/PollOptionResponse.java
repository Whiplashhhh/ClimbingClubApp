package app.belay.poll.dto;

import java.util.UUID;

/** Une option d'un sondage avec son décompte de voix. */
public record PollOptionResponse(UUID id, String label, long votes) {}
