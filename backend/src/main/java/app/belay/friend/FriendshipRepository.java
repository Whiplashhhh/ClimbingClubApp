package app.belay.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /** Le lien entre deux membres, quel que soit le sens de la demande. */
    @Query("""
            select f from Friendship f
            where f.organization.id = :organizationId
              and ((f.requester.id = :a and f.addressee.id = :b)
                   or (f.requester.id = :b and f.addressee.id = :a))
            """)
    Optional<Friendship> findBetween(
            @Param("organizationId") UUID organizationId, @Param("a") UUID a, @Param("b") UUID b);

    /** Amitiés acceptées impliquant l'utilisateur (dans un sens ou l'autre). */
    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where f.status = app.belay.friend.FriendshipStatus.ACCEPTED
              and (f.requester.id = :userId or f.addressee.id = :userId)
            order by f.updatedAt desc
            """)
    List<Friendship> findAcceptedOf(@Param("userId") UUID userId);

    /** Demandes en attente reçues par l'utilisateur. */
    @Query("""
            select f from Friendship f
            join fetch f.requester
            where f.status = app.belay.friend.FriendshipStatus.PENDING and f.addressee.id = :userId
            order by f.createdAt desc
            """)
    List<Friendship> findIncomingPending(@Param("userId") UUID userId);
}
