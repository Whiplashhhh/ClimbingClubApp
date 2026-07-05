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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Dernière lecture d'un fil par un participant : sert au décompte des non-lus (groupes compris). */
@Entity
@Table(name = "conversation_read")
public class ConversationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    protected ConversationRead() {}

    public ConversationRead(Organization organization, Conversation conversation, AppUser user, Instant lastReadAt) {
        this.organization = organization;
        this.conversation = conversation;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }

    public void markReadAt(Instant at) {
        this.lastReadAt = at;
    }

    public UUID getId() {
        return id;
    }
}
