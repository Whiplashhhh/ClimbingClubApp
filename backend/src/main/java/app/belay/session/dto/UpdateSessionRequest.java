package app.belay.session.dto;

import app.belay.session.SessionVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSessionRequest(
        @Size(max = 500) String note, @NotNull SessionVisibility visibility) {}
