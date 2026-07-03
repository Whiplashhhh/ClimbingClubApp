package app.belay.post.dto;

import app.belay.post.PostAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePosterRequest(
        @NotNull PostAudience audience,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String body) {}
