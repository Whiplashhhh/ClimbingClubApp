package app.belay.post;

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
import java.util.UUID;

/**
 * Publication du fil : titre + texte + 0..n images ({@link PostImage}). Pas de catégorie —
 * les posts générés par une annulation de séance portent {@code important} (mise en évidence)
 * et {@code pinnedUntil} (épinglés en tête du fil jusqu'à la fin du jour de la séance).
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostAudience audience;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String body;

    @Column(nullable = false)
    private boolean important;

    @Column(name = "pinned_until")
    private Instant pinnedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Post() {}

    public Post(
            Organization organization,
            AppUser author,
            PostAudience audience,
            String title,
            String body,
            boolean important,
            Instant pinnedUntil) {
        this.organization = organization;
        this.author = author;
        this.audience = audience;
        this.title = title;
        this.body = body;
        this.important = important;
        this.pinnedUntil = pinnedUntil;
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

    public Organization getOrganization() {
        return organization;
    }

    public AppUser getAuthor() {
        return author;
    }

    public PostAudience getAudience() {
        return audience;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isImportant() {
        return important;
    }

    public Instant getPinnedUntil() {
        return pinnedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
