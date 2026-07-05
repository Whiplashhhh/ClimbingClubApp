package app.belay.poll;

import app.belay.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Une réponse possible d'un sondage, ordonnée par {@code position}. */
@Entity
@Table(name = "poll_option")
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false)
    private short position;

    protected PollOption() {}

    public PollOption(Organization organization, Poll poll, String label, short position) {
        this.organization = organization;
        this.poll = poll;
        this.label = label;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public Poll getPoll() {
        return poll;
    }

    public String getLabel() {
        return label;
    }

    public short getPosition() {
        return position;
    }
}
