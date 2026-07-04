package app.belay.route.dto;

import app.belay.route.ClimbType;
import app.belay.route.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RouteResponse(
        UUID id,
        UUID sectorId,
        String name,
        String grade,
        ClimbType climbType,

        @Schema(description = "Short-lived signed URL of the route photo")
        String photoUrl,

        @Schema(description = "Highlighted holds, relative coordinates (0..1), overlay-rendered")
        List<HoldDto> holds,

        UUID createdById,
        String createdByDisplayName,
        Instant createdAt) {

    public static RouteResponse from(Route route, String photoUrl) {
        List<HoldDto> holds = route.getHolds() == null
                ? List.of()
                : route.getHolds().stream().map(HoldDto::from).toList();
        return new RouteResponse(
                route.getId(),
                route.getSector().getId(),
                route.getName(),
                route.getGrade(),
                route.getClimbType(),
                photoUrl,
                holds,
                route.getCreatedBy().getId(),
                route.getCreatedBy().getDisplayName(),
                route.getCreatedAt());
    }
}
