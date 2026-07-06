package app.belay.storage;

import app.belay.common.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stockage objet des images : contenu vérifié par octets magiques, clé regénérée (anti
 * path-traversal — aucun nom de fichier client ne touche la clé), bucket privé jamais exposé.
 * La lecture passe par l'application ({@code GET /api/media/**}, authentifié et scopé par
 * organisation) — voir ADR 0006 (révisé) : plus d'URLs signées, donc aucun hôte MinIO à
 * configurer côté client et aucune fuite hors session.
 */
@Service
public class StorageService {

    /** Préfixe des URLs de lecture servies par l'application (même origine que l'API). */
    public static final String MEDIA_URL_PREFIX = "/api/media/";

    private final S3Client s3Client;
    private final StorageProperties properties;

    private volatile boolean bucketReady = false;

    public StorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /** Contenu et type d'un objet, tel que servi par {@code /api/media/**}. */
    public record StoredMedia(byte[] content, String contentType) {}

    /**
     * Valide le contenu (image JPEG/PNG/WebP uniquement) et le stocke sous une clé regénérée
     * {@code keyPrefix/UUID.ext}.
     *
     * @return la clé objet à persister
     * @throws IllegalArgumentException si le contenu n'est pas une image supportée
     */
    public String storeImage(byte[] content, String keyPrefix) {
        ImageFormat format = ImageFormat.detect(content)
                .orElseThrow(() ->
                        new IllegalArgumentException("File content is not a supported image (JPEG, PNG or WebP)"));
        ensureBucket();
        String key = keyPrefix + "/" + UUID.randomUUID() + "." + format.extension();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(key)
                        .contentType(format.contentType())
                        .build(),
                RequestBody.fromBytes(content));
        return key;
    }

    /** URL de lecture relative à l'application — valable quel que soit l'hôte du déploiement. */
    public String publicUrl(String objectKey) {
        return MEDIA_URL_PREFIX + objectKey;
    }

    /** URL de lecture, ou {@code null} si aucune clé (ex. avatar absent). */
    public String publicUrlOrNull(String objectKey) {
        return objectKey == null ? null : publicUrl(objectKey);
    }

    public StoredMedia fetch(String objectKey) {
        try {
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
            return new StoredMedia(bytes.asByteArray(), bytes.response().contentType());
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            throw new NotFoundException("Media not found");
        }
    }

    public void delete(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build());
    }

    /** Crée le bucket au premier usage (idempotent) — évite de coupler le démarrage à MinIO. */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                s3Client.headBucket(
                        HeadBucketRequest.builder().bucket(properties.bucket()).build());
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket(properties.bucket())
                        .build());
            }
            bucketReady = true;
        }
    }
}
