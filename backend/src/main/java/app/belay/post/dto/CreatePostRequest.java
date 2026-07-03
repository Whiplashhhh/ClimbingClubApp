package app.belay.post.dto;

import app.belay.post.PostAudience;
import app.belay.post.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull @Schema(description = "INFO or CANCELLATION — posters go through POST /api/posts/poster")
        PostType type,

        @NotNull PostAudience audience,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String body) {}
