package app.belay.slot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotMembershipRepository extends JpaRepository<SlotMembership, UUID> {

    @Query("""
            select m from SlotMembership m
            join fetch m.user
            where m.organization.id = :organizationId
            """)
    List<SlotMembership> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    List<SlotMembership> findAllBySlotId(UUID slotId);

    boolean existsBySlotIdAndUserId(UUID slotId, UUID userId);

    Optional<SlotMembership> findBySlotIdAndUserId(UUID slotId, UUID userId);
}
