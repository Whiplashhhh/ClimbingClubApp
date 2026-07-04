package app.belay.storage;

import app.belay.auth.UserPrincipal;
import app.belay.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sert les images stockées dans MinIO à travers l'application : même origine que l'API,
 * session requise, et isolation multi-tenant par le préfixe de clé
 * ({@code <domaine>/<organizationId>/<uuid>.<ext>}) — une clé d'une autre organisation → 404.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "media", description = "Authenticated, tenant-scoped delivery of stored images")
public class MediaController {

    private static final Set<String> KNOWN_PREFIXES = Set.of("posts", "sectors", "routes");

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/media/{*objectKey}")
    @Operation(summary = "Stream a stored image (only keys belonging to the caller's organization)")
    @ApiResponse(responseCode = "404", description = "Unknown key or key outside the caller's organization")
    public ResponseEntity<byte[]> get(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable String objectKey) {
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        requireOwnedByOrganization(principal, key);
        StorageService.StoredMedia media = storageService.fetch(key);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(media.contentType());
        } catch (RuntimeException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        // Clés à usage unique (UUID) : le contenu est immuable, le cache privé long est sûr
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .body(media.content());
    }

    /** Format attendu : {@code <domaine>/<organizationId>/...} — tout écart est masqué en 404. */
    private void requireOwnedByOrganization(UserPrincipal principal, String key) {
        String[] segments = key.split("/");
        if (segments.length < 3
                || !KNOWN_PREFIXES.contains(segments[0])
                || !segments[1].equals(principal.organizationId().toString())) {
            throw new NotFoundException("Media not found");
        }
    }
}
