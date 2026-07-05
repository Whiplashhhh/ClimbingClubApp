package app.belay.common;

/** Débit dépassé (ex. limite d'écriture du groupe général) → HTTP 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
