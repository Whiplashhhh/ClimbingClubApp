package app.belay.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, UUID> {

    List<Sector> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<Sector> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
