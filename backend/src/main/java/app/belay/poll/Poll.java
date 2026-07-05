package app.belay.poll;

import app.belay.organization.Organization;
import app.belay.post.PostAudience;
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
 * Sondage à choix unique, partageant l'audience du fil ({@link PostAudience} : ORG ou
 * COACH_STUDENTS). Les options vivent dans {@link PollOption}, les réponses dans {@link PollVote}.
 * Une échéance optionnelle ({@code closesAt}) ferme le vote au-delà de la date.
 */
@Entity
@Table(name = "poll")
public class Poll {

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

    @Column(nullable = false, length = 300)
    private String question;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Poll() {}

    public Poll(Organization organization, AppUser author, PostAudience audience, String question, Instant closesAt) {
        this.organization = organization;
        this.author = author;
        this.audience = audience;
        this.question = question;
        this.closesAt = closesAt;
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

    public boolean isClosed() {
        return closesAt != null && closesAt.isBefore(Instant.now());
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

    public String getQuestion() {
        return question;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
