package app.belay.post;

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

/** Image jointe à un post, ordonnée par {@code position} (clé objet MinIO, servie en URL signée). */
@Entity
@Table(name = "post_image")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(nullable = false)
    private int position;

    protected PostImage() {}

    public PostImage(Organization organization, Post post, String objectKey, int position) {
        this.organization = organization;
        this.post = post;
        this.objectKey = objectKey;
        this.position = position;
    }

    public Post getPost() {
        return post;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public int getPosition() {
        return position;
    }
}
