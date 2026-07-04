package app.belay.slot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    @Query("select s from Slot s join fetch s.coach where s.organization.id = :organizationId")
    List<Slot> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    Optional<Slot> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
