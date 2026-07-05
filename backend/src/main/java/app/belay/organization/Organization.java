package app.belay.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 140, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "climbing_type", nullable = false, length = 20)
    private ClimbingType climbingType;

    @Column(length = 255)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(name = "logo_object_key", length = 255)
    private String logoObjectKey;

    // Limite d'écriture du groupe général : NULL = illimité ; sinon N messages / fenêtre (s).
    @Column(name = "general_chat_rate_limit")
    private Integer generalChatRateLimit;

    @Column(name = "general_chat_window_seconds")
    private Integer generalChatWindowSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Organization() {}

    public Organization(String name, String slug, ClimbingType climbingType) {
        this.name = name;
        this.slug = slug;
        this.climbingType = climbingType;
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

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public ClimbingType getClimbingType() {
        return climbingType;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getLogoObjectKey() {
        return logoObjectKey;
    }

    public Integer getGeneralChatRateLimit() {
        return generalChatRateLimit;
    }

    public Integer getGeneralChatWindowSeconds() {
        return generalChatWindowSeconds;
    }

    /** Définit la limite du groupe général ({@code null, null} = illimité). */
    public void setGeneralChatLimit(Integer rateLimit, Integer windowSeconds) {
        this.generalChatRateLimit = rateLimit;
        this.generalChatWindowSeconds = windowSeconds;
    }
}
