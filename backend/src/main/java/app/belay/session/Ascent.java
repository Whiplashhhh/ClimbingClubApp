package app.belay.session;

import app.belay.organization.Organization;
import app.belay.route.Route;
import app.belay.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Ascension d'une voie au sein d'une séance : appréciation, prise la plus haute atteinte, temps
 * (optionnel), et assureur (un ami — Phase 5B — OU un nom libre).
 */
@Entity
@Table(name = "ascent")
public class Ascent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClimbingSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    private Short rating;

    @Column(name = "top_hold")
    private Short topHold;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "belayer_user_id")
    private AppUser belayerUser;

    @Column(name = "belayer_name", length = 120)
    private String belayerName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Ascent() {}

    public Ascent(
            Organization organization,
            ClimbingSession session,
            Route route,
            Short rating,
            Short topHold,
            Integer durationSeconds,
            String belayerName) {
        this.organization = organization;
        this.session = session;
        this.route = route;
        this.rating = rating;
        this.topHold = topHold;
        this.durationSeconds = durationSeconds;
        this.belayerName = belayerName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ClimbingSession getSession() {
        return session;
    }

    public Route getRoute() {
        return route;
    }

    public Short getRating() {
        return rating;
    }

    public Short getTopHold() {
        return topHold;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getBelayerName() {
        return belayerName;
    }

    public AppUser getBelayerUser() {
        return belayerUser;
    }
}
