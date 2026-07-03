package app.belay.storage;

import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Stockage objet des images : contenu vérifié par octets magiques, clé regénérée (anti
 * path-traversal — aucun nom de fichier client ne touche la clé), lecture via URL signée à durée
 * limitée. Le bucket est privé : rien n'est servi directement.
 */
@Service
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    private volatile boolean bucketReady = false;

    public StorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

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

    /** URL signée de lecture, à durée limitée (belay.storage.presign-ttl). */
    public String presignGet(String objectKey) {
        Duration ttl = properties.presignTtl();
        var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .build())
                .build());
        return presigned.url().toString();
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
