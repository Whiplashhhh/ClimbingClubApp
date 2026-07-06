package app.belay.auth;

import app.belay.auth.dto.RegisterRequest;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.organization.Organization;
import app.belay.organization.OrganizationRepository;
import app.belay.storage.StorageService;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import app.belay.user.UserStatus;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            StorageService storageService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        if (request.isCreateMode() == request.isJoinMode()) {
            throw new IllegalArgumentException("Provide exactly one of createOrganization or joinSlug");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }

        Organization organization;
        Role role;
        UserStatus status;
        if (request.isCreateMode()) {
            organization = organizationRepository.save(new Organization(
                    request.createOrganization().name().trim(),
                    generateSlug(request.createOrganization().name()),
                    request.createOrganization().climbingType()));
            role = Role.OWNER;
            status = UserStatus.ACTIVE;
        } else {
            organization = organizationRepository
                    .findBySlug(request.joinSlug().trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new NotFoundException("Unknown organization"));
            role = Role.MEMBER;
            status = UserStatus.PENDING;
        }

        return userRepository.save(new AppUser(
                organization,
                request.email().trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                role,
                status));
    }

    /** Change le mot de passe après vérification de l'actuel (sinon 400). */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    /** Met à jour nom affiché et/ou email (confirmé par le mot de passe actuel). Email unique global. */
    @Transactional
    public AppUser updateProfile(UUID userId, String displayName, String email, String currentPassword) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName.trim());
        }
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (!normalized.equals(user.getEmail()) && userRepository.existsByEmailIgnoreCase(normalized)) {
                throw new ConflictException("Email already registered");
            }
            user.setEmail(normalized);
        }
        return user;
    }

    /** Remplace la photo de profil (image validée par contenu) ; l'ancienne est supprimée. */
    @Transactional
    public AppUser updateAvatar(UUID userId, byte[] content) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String oldKey = user.getAvatarObjectKey();
        String key = storageService.storeImage(
                content, "avatars/" + user.getOrganization().getId());
        user.setAvatarObjectKey(key);
        if (oldKey != null) {
            storageService.delete(oldKey);
        }
        return user;
    }

    private String generateSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "club";
        }
        String slug = base;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }
}
