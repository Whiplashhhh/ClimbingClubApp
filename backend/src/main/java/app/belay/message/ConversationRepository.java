package app.belay.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByCoachIdAndStudentId(UUID coachId, UUID studentId);

    /** Mes fils (comme moniteur ou comme élève), plus actifs d'abord. */
    @Query("""
            select c from Conversation c
            join fetch c.coach
            join fetch c.student
            where c.organization.id = :organizationId
              and (c.coach.id = :userId or c.student.id = :userId)
            order by c.lastMessageAt desc, c.id desc
            """)
    List<Conversation> findAllForUser(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    /** Un fil précis, seulement si l'appelant en est un participant (sinon vide → 404). */
    @Query("""
            select c from Conversation c
            join fetch c.coach
            join fetch c.student
            where c.id = :id
              and c.organization.id = :organizationId
              and (c.coach.id = :userId or c.student.id = :userId)
            """)
    Optional<Conversation> findVisible(
            @Param("id") UUID id, @Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

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
}
