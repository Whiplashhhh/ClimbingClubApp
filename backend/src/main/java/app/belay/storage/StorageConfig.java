package app.belay.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    // Path-style obligatoire avec MinIO (pas de DNS par bucket)
    private static final S3Configuration PATH_STYLE =
            S3Configuration.builder().pathStyleAccessEnabled(true).build();

    @Bean
    StaticCredentialsProvider s3Credentials(StorageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }

    @Bean
    S3Client s3Client(StorageProperties properties, StaticCredentialsProvider credentials) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials)
                .serviceConfiguration(PATH_STYLE)
                .build();
    }

    /** Presigner sur l'endpoint public : l'URL signée doit être valide depuis le navigateur. */
    @Bean
    S3Presigner s3Presigner(StorageProperties properties, StaticCredentialsProvider credentials) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.publicEndpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials)
                .serviceConfiguration(PATH_STYLE)
                .build();
    }
}
