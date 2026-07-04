package app.belay.route.dto;

import app.belay.route.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Prise mise en évidence, en coordonnées relatives à la photo (0..1). */
@Schema(description = "Highlighted hold, in coordinates relative to the photo (0..1)")
public record HoldDto(
        @DecimalMin("0") @DecimalMax("1") double x,
        @DecimalMin("0") @DecimalMax("1") double y) {

    public static HoldDto from(Route.Hold hold) {
        return new HoldDto(hold.x(), hold.y());
    }

    public Route.Hold toHold() {
        return new Route.Hold(x, y);
    }
}
