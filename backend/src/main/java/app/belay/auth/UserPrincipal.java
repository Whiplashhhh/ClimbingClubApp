package app.belay.auth;

import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal stocké en session : identifiants minimaux (pas d'entité JPA). Le rôle et le statut
 * sont resynchronisés depuis la base à chaque requête par {@link AccountRefreshFilter} — une
 * désactivation ou un changement de rôle prend effet immédiatement.
 */
public record UserPrincipal(
        UUID id, UUID organizationId, String email, String passwordHash, Role role, UserStatus status)
        implements UserDetails {

    public static UserPrincipal from(AppUser user) {
        return new UserPrincipal(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus());
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name()),
                new SimpleGrantedAuthority("STATUS_" + status.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status != UserStatus.DISABLED;
    }
}
