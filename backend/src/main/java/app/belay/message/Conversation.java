package app.belay.message;

import app.belay.organization.Organization;
import app.belay.slot.Slot;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Fil de discussion. Trois formes ({@link ConversationType}) : DIRECT (paire moniteur/élève),
 * SLOT (groupe d'un créneau) et GENERAL (groupe du club). Pour les groupes, l'accès est *calculé*
 * (appartenance au créneau / au club), pas stocké : un nouvel arrivant voit tout l'historique.
 * {@code lastMessageAt} porte le tri par activité récente.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    // DIRECT : moniteur et élève. Null pour les groupes.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private AppUser coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private AppUser student;

    // SLOT : le créneau associé. Null sinon.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private Slot slot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    protected Conversation() {}

    private Conversation(Organization organization, ConversationType type) {
        this.organization = organization;
        this.type = type;
    }

    public static Conversation direct(Organization organization, AppUser coach, AppUser student) {
        Conversation c = new Conversation(organization, ConversationType.DIRECT);
        c.coach = coach;
        c.student = student;
        return c;
    }

    public static Conversation forSlot(Organization organization, Slot slot) {
        Conversation c = new Conversation(organization, ConversationType.SLOT);
        c.slot = slot;
        return c;
    }

    public static Conversation general(Organization organization) {
        return new Conversation(organization, ConversationType.GENERAL);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        lastMessageAt = createdAt;
    }

    public void touch(Instant at) {
        this.lastMessageAt = at;
    }

    /** L'autre participant d'un fil DIRECT, du point de vue de {@code userId}. */
    public AppUser directOther(UUID userId) {
        return coach.getId().equals(userId) ? student : coach;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public ConversationType getType() {
        return type;
    }

    public AppUser getCoach() {
        return coach;
    }

    public AppUser getStudent() {
        return student;
    }

    public Slot getSlot() {
        return slot;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }
}
