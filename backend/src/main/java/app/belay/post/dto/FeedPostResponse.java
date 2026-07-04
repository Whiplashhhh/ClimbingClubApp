package app.belay.post.dto;

import app.belay.post.Post;
import app.belay.post.PostAudience;
import app.belay.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedPostResponse(
        UUID id,
        PostAudience audience,
        String title,
        String body,

        @Schema(description = "Short-lived signed URLs of the attached images, in display order")
        List<String> imageUrls,

        @Schema(description = "Highlighted in the feed (e.g. session cancellations)")
        boolean important,

        @Schema(description = "Kept at the top of the feed until this instant")
        Instant pinnedUntil,

        UUID authorId,
        String authorDisplayName,
        Role authorRole,
        Instant createdAt) {

    public static FeedPostResponse from(Post post, List<String> imageUrls) {
        return new FeedPostResponse(
                post.getId(),
                post.getAudience(),
                post.getTitle(),
                post.getBody(),
                imageUrls,
                post.isImportant(),
                post.getPinnedUntil(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getRole(),
                post.getCreatedAt());
    }
}
