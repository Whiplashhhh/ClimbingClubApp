package app.belay.message;

import app.belay.organization.Organization;
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
 * Fil de discussion privé entre un moniteur et l'un de ses élèves (relation dérivée d'un créneau,
 * A-005). Un seul fil par paire ; {@code lastMessageAt} porte le tri par activité récente.
 */
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private AppUser coach;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    protected Conversation() {}

    public Conversation(Organization organization, AppUser coach, AppUser student) {
        this.organization = organization;
        this.coach = coach;
        this.student = student;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        lastMessageAt = createdAt;
    }

    public void touch(Instant at) {
        this.lastMessageAt = at;
    }

    /** L'autre participant du fil, du point de vue de {@code userId}. */
    public AppUser other(UUID userId) {
        return coach.getId().equals(userId) ? student : coach;
    }

    public boolean involves(UUID userId) {
        return coach.getId().equals(userId) || student.getId().equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public AppUser getCoach() {
        return coach;
    }

    public AppUser getStudent() {
        return student;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }
}
