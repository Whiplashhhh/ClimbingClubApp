package app.belay.post;

public enum PostAudience {
    /** Toute l'organisation — réservé aux OWNER/ADMIN. */
    ORG,
    /** Les élèves du moniteur auteur (via SlotMembership, Phase 3 — voir A-005). */
    COACH_STUDENTS
}
