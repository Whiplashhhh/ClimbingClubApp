package app.belay.session.dto;

import app.belay.session.Ascent;
import java.util.UUID;

public record AscentResponse(
        UUID id,
        UUID routeId,
        String routeName,
        String routeGrade,
        Short rating,
        Short topHold,
        Integer durationSeconds,
        String belayerName) {

    public static AscentResponse from(Ascent ascent) {
        // L'assureur ami (Phase 5B) est présenté par son nom d'affichage ; sinon le nom libre
        String belayer =
                ascent.getBelayerUser() != null ? ascent.getBelayerUser().getDisplayName() : ascent.getBelayerName();
        return new AscentResponse(
                ascent.getId(),
                ascent.getRoute().getId(),
                ascent.getRoute().getName(),
                ascent.getRoute().getGrade(),
                ascent.getRating(),
                ascent.getTopHold(),
                ascent.getDurationSeconds(),
                belayer);
    }
}
