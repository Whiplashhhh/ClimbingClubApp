package app.belay.poll.dto;

import app.belay.poll.Poll;
import app.belay.post.PostAudience;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Un sondage tel que présenté à l'utilisateur : question, options avec décomptes, mon choix
 * courant ({@code myOptionId}, null si je n'ai pas voté), total des voix, et état de fermeture.
 */
public record PollResponse(
        UUID id,
        UUID authorId,
        String authorDisplayName,
        PostAudience audience,
        String question,
        Instant closesAt,
        boolean closed,
        Instant createdAt,
        List<PollOptionResponse> options,
        UUID myOptionId,
        long totalVotes) {

    public static PollResponse from(Poll poll, List<PollOptionResponse> options, UUID myOptionId) {
        long total = options.stream().mapToLong(PollOptionResponse::votes).sum();
        return new PollResponse(
                poll.getId(),
                poll.getAuthor().getId(),
                poll.getAuthor().getDisplayName(),
                poll.getAudience(),
                poll.getQuestion(),
                poll.getClosesAt(),
                poll.isClosed(),
                poll.getCreatedAt(),
                options,
                myOptionId,
                total);
    }
}
