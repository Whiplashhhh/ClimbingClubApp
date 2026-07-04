package app.belay.slot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotChangeRepository extends JpaRepository<SlotChange, UUID> {

    /** Modifications à venir (>= from) de tous les créneaux de l'organisation. */
    @Query("""
            select c from SlotChange c
            where c.organization.id = :organizationId and c.date >= :from
            order by c.date asc
            """)
    List<SlotChange> findUpcomingByOrganizationId(
            @Param("organizationId") UUID organizationId, @Param("from") LocalDate from);

    List<SlotChange> findAllBySlotIdAndDateGreaterThanEqualOrderByDateAsc(UUID slotId, LocalDate from);

    boolean existsBySlotIdAndDate(UUID slotId, LocalDate date);

    Optional<SlotChange> findByIdAndSlotId(UUID id, UUID slotId);

    List<SlotChange> findAllBySlotId(UUID slotId);
}
