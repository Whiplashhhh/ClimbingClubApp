package app.belay.organization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Recherche globale (non scopée) assumée : le slug sert de point d'entrée public pour
     * rejoindre une organisation à l'inscription, avant tout rattachement.
     */
    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
