package app.belay.poll.dto;

import app.belay.post.PostAudience;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreatePollRequest(
        @NotNull PostAudience audience,
        @NotBlank @Size(max = 300) String question,

        @Schema(description = "Optional closing date; votes are refused past it")
        Instant closesAt,

        @NotNull @Size(min = 2, max = 10, message = "A poll needs between 2 and 10 options")
        List<@NotBlank @Size(max = 200) String> options) {}
