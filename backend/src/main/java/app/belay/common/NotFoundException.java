package app.belay.common;

/**
 * Ressource introuvable — aussi renvoyée quand la ressource existe dans une autre organisation,
 * pour ne pas révéler son existence (cf. ADR 0004).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
