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
    private PostType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostAudience audience;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String body;

    @Column(name = "image_object_key", length = 255)
    private String imageObjectKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Post() {}

    public Post(
            Organization organization,
            AppUser author,
            PostType type,
            PostAudience audience,
            String title,
            String body,
            String imageObjectKey) {
        this.organization = organization;
        this.author = author;
        this.type = type;
        this.audience = audience;
        this.title = title;
        this.body = body;
        this.imageObjectKey = imageObjectKey;
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

    public PostType getType() {
        return type;
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

    public String getImageObjectKey() {
        return imageObjectKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
