package app.belay.session;

public enum SessionVisibility {
    /** Visible par tout le club. */
    CLUB,
    /** Visible par les amis (Phase 5B — d'ici là, équivaut à PRIVATE). */
    FRIENDS,
    /** Visible par le seul auteur. */
    PRIVATE
}
