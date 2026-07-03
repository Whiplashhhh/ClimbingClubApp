package app.belay.storage;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du stockage objet S3-compatible (MinIO en local/compose).
 *
 * <p>{@code endpoint} est l'URL vue par le backend ; {@code publicEndpoint} celle vue par le
 * navigateur pour les URLs signées (dans docker-compose le back parle à {@code http://minio:9000}
 * mais le client télécharge via {@code http://localhost:9000}).
 */
@Validated
@ConfigurationProperties(prefix = "belay.storage")
public record StorageProperties(
        @NotBlank String endpoint,
        String publicEndpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @NotBlank String region,
        Duration presignTtl) {

    public StorageProperties {
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            publicEndpoint = endpoint;
        }
        if (presignTtl == null) {
            presignTtl = Duration.ofMinutes(15);
        }
    }
}
