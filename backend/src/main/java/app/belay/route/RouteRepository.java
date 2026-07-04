package app.belay.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteRepository extends JpaRepository<Route, UUID> {

    @Query("""
            select r from Route r
            join fetch r.createdBy
            where r.organization.id = :organizationId
            order by r.name asc
            """)
    List<Route> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    Optional<Route> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countBySectorId(UUID sectorId);
}
