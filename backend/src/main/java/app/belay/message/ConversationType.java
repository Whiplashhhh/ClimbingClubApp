package app.belay.message;

public enum ConversationType {
    /** Fil privé 1:1 entre un moniteur et l'un de ses élèves. */
    DIRECT,
    /** Groupe d'un créneau : ses membres et son moniteur. */
    SLOT,
    /** Groupe général du club : tous les membres actifs. */
    GENERAL
}
