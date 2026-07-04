package app.belay.route.dto;

import app.belay.route.Sector;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record SectorResponse(
        UUID id,
        String name,

        @Schema(description = "Short-lived signed URL of the wall photo")
        String photoUrl,

        List<RouteResponse> routes) {

    public static SectorResponse from(Sector sector, String photoUrl, List<RouteResponse> routes) {
        return new SectorResponse(sector.getId(), sector.getName(), photoUrl, routes);
    }
}
