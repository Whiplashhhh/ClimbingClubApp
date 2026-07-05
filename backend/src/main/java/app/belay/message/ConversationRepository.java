package app.belay.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Conversation> findByOrganizationIdAndType(UUID organizationId, ConversationType type);

    Optional<Conversation> findBySlotId(UUID slotId);

    // --- DIRECT (1:1) ---

    Optional<Conversation> findByCoachIdAndStudentId(UUID coachId, UUID studentId);

    /** Mes fils 1:1 (comme moniteur ou comme élève), les deux participants chargés. */
    @Query("""
            select c from Conversation c
            join fetch c.coach
            join fetch c.student
            where c.type = app.belay.message.ConversationType.DIRECT
              and c.organization.id = :organizationId
              and (c.coach.id = :userId or c.student.id = :userId)
            """)
    List<Conversation> findDirectForUser(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    /** Relation moniteur→élève : l'élève est rattaché à un créneau du moniteur (A-005). */
    @Query("""
            select (count(m) > 0) from SlotMembership m
            where m.organization.id = :organizationId
              and m.slot.coach.id = :coachId
              and m.user.id = :studentId
            """)
    boolean existsCoachStudentRelation(
            @Param("organizationId") UUID organizationId,
            @Param("coachId") UUID coachId,
            @Param("studentId") UUID studentId);

    // --- SLOT groups ---

    /** Créneaux dont l'utilisateur fait partie : son moniteur ou l'un de ses élèves rattachés. */
    @Query("""
            select s.id from Slot s
            where s.organization.id = :organizationId
              and (s.coach.id = :userId
                   or exists (select 1 from SlotMembership m where m.slot.id = s.id and m.user.id = :userId))
            """)
    List<UUID> findSlotIdsForUser(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    @Query("""
            select c from Conversation c
            join fetch c.slot
            where c.type = app.belay.message.ConversationType.SLOT and c.slot.id in :slotIds
            """)
    List<Conversation> findSlotConversations(@Param("slotIds") List<UUID> slotIds);

    /** L'utilisateur participe au groupe d'un créneau : moniteur ou élève rattaché. */
    @Query("""
            select (count(s) > 0) from Slot s
            where s.id = :slotId
              and (s.coach.id = :userId
                   or exists (select 1 from SlotMembership m where m.slot.id = s.id and m.user.id = :userId))
            """)
    boolean isSlotParticipant(@Param("slotId") UUID slotId, @Param("userId") UUID userId);
}
