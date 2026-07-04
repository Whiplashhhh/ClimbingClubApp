package app.belay.route;

import app.belay.organization.Organization;
import app.belay.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Voie (ou bloc) rattachée à un secteur. Les prises sont des annotations vectorielles en
 * coordonnées relatives à la photo (0..1), stockées en JSONB et rendues en overlay côté front —
 * le fichier image n'est jamais modifié.
 */
@Entity
@Table(name = "route")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 10)
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "climb_type", nullable = false, length = 20)
    private ClimbType climbType;

    @Column(name = "photo_object_key", length = 255)
    private String photoObjectKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Hold> holds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Prise mise en évidence, en coordonnées relatives à l'image (0..1). */
    public record Hold(double x, double y) {}

    protected Route() {}

    public Route(
            Organization organization,
            Sector sector,
            AppUser createdBy,
            String name,
            String grade,
            ClimbType climbType,
            String photoObjectKey) {
        this.organization = organization;
        this.sector = sector;
        this.createdBy = createdBy;
        this.name = name;
        this.grade = grade;
        this.climbType = climbType;
        this.photoObjectKey = photoObjectKey;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Sector getSector() {
        return sector;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getGrade() {
        return grade;
    }

    public ClimbType getClimbType() {
        return climbType;
    }

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public List<Hold> getHolds() {
        return holds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setHolds(List<Hold> holds) {
        this.holds = holds;
    }
}
