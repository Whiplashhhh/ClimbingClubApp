package app.belay.slot;

import app.belay.organization.Organization;
import app.belay.post.Post;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Annulation ou décalage d'UNE séance d'un créneau, à une date donnée. */
@Entity
@Table(name = "slot_change")
public class SlotChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotChangeAction action;

    @Column(name = "new_start_time")
    private LocalTime newStartTime;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    /** Post « Cours annulé » publié automatiquement dans le fil (retiré si la séance est rétablie). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SlotChange() {}

    public SlotChange(
            Organization organization,
            Slot slot,
            LocalDate date,
            SlotChangeAction action,
            LocalTime newStartTime,
            String note,
            AppUser createdBy) {
        this.organization = organization;
        this.slot = slot;
        this.date = date;
        this.action = action;
        this.newStartTime = newStartTime;
        this.note = note;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Slot getSlot() {
        return slot;
    }

    public LocalDate getDate() {
        return date;
    }

    public SlotChangeAction getAction() {
        return action;
    }

    public LocalTime getNewStartTime() {
        return newStartTime;
    }

    public String getNote() {
        return note;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}
