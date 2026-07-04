package app.belay.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du stockage objet S3-compatible (MinIO en local/compose). L'endpoint n'est vu
 * que par le backend : la lecture côté client passe par {@code /api/media/**}.
 */
@Validated
@ConfigurationProperties(prefix = "belay.storage")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @NotBlank String region) {}
