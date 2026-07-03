package app.belay.post.dto;

import app.belay.post.Post;
import app.belay.post.PostAudience;
import app.belay.post.PostType;
import app.belay.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record FeedPostResponse(
        UUID id,
        PostType type,
        PostAudience audience,
        String title,
        String body,

        @Schema(description = "Short-lived signed URL of the poster image (POSTER posts only)")
        String imageUrl,

        UUID authorId,
        String authorDisplayName,
        Role authorRole,
        Instant createdAt) {

    public static FeedPostResponse from(Post post, String imageUrl) {
        return new FeedPostResponse(
                post.getId(),
                post.getType(),
                post.getAudience(),
                post.getTitle(),
                post.getBody(),
                imageUrl,
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getRole(),
                post.getCreatedAt());
    }
}
