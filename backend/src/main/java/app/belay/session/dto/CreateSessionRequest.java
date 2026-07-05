package app.belay.session.dto;

import app.belay.session.SessionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateSessionRequest(
        @Schema(description = "Defaults to now if omitted") Instant startedAt,
        @Size(max = 500) String note,
        @NotNull SessionVisibility visibility) {}
