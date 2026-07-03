package app.belay.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    /** Recherche globale (non scopée) assumée : authentification par email, avant session. */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Toutes les méthodes ci-dessous sont scopées par organisation (isolation multi-tenant).

    Optional<AppUser> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<AppUser> findAllByOrganizationIdAndStatusOrderByDisplayNameAsc(UUID organizationId, UserStatus status);
}
