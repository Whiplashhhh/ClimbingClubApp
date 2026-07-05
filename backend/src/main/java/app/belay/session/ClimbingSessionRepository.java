package app.belay.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClimbingSessionRepository extends JpaRepository<ClimbingSession, UUID> {

    @Query("""
            select s from ClimbingSession s
            join fetch s.user
            where s.user.id = :userId
            order by s.startedAt desc, s.id desc
            """)
    List<ClimbingSession> findMine(@Param("userId") UUID userId);

    Optional<ClimbingSession> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** Fil d'activité : mes séances (toutes) + les séances CLUB des autres, plus récentes d'abord. */
    @Query("""
            select s from ClimbingSession s
            join fetch s.user
            where s.organization.id = :organizationId
              and (s.user.id = :userId or s.visibility = app.belay.session.SessionVisibility.CLUB)
            order by s.startedAt desc, s.id desc
            """)
    List<ClimbingSession> findClubActivity(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);
}
