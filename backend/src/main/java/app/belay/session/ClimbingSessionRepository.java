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

    /**
     * Fil d'activité : mes séances (toutes) + les séances CLUB des autres + les séances FRIENDS
     * de mes amis (amitié acceptée dans un sens ou l'autre), plus récentes d'abord.
     */
    @Query("""
            select s from ClimbingSession s
            join fetch s.user
            where s.organization.id = :organizationId
              and (s.user.id = :userId
                   or s.visibility = app.belay.session.SessionVisibility.CLUB
                   or (s.visibility = app.belay.session.SessionVisibility.FRIENDS
                       and exists (select 1 from Friendship f
                                   where f.status = app.belay.friend.FriendshipStatus.ACCEPTED
                                     and ((f.requester.id = :userId and f.addressee.id = s.user.id)
                                          or (f.addressee.id = :userId and f.requester.id = s.user.id)))))
            order by s.startedAt desc, s.id desc
            """)
    List<ClimbingSession> findClubActivity(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);
}
