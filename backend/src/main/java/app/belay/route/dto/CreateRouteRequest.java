package app.belay.route.dto;

import app.belay.route.ClimbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateRouteRequest(
        @NotNull UUID sectorId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 10) String grade,
        @NotNull ClimbType climbType) {}
