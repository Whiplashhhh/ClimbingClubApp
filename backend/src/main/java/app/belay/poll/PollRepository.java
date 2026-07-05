package app.belay.poll;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollRepository extends JpaRepository<Poll, UUID> {

    /**
     * Sondages visibles par l'utilisateur : ceux de son organisation dont il fait partie de
     * l'audience. Même prédicat que le fil (A-005) : ORG = tout le monde ; COACH_STUDENTS =
     * l'auteur et les élèves du moniteur auteur (membres d'un de ses créneaux). Plus récents d'abord.
     */
    @Query("""
            select p from Poll p
            join fetch p.author
            where p.organization.id = :organizationId
              and (p.audience = app.belay.post.PostAudience.ORG
                   or p.author.id = :userId
                   or exists (select 1 from SlotMembership m
                              where m.user.id = :userId and m.slot.coach.id = p.author.id))
            order by p.createdAt desc, p.id desc
            """)
    List<Poll> findAllVisible(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    /** Un sondage précis, seulement s'il est visible par l'utilisateur (sinon vide → 404). */
    @Query("""
            select p from Poll p
            join fetch p.author
            where p.id = :pollId
              and p.organization.id = :organizationId
              and (p.audience = app.belay.post.PostAudience.ORG
                   or p.author.id = :userId
                   or exists (select 1 from SlotMembership m
                              where m.user.id = :userId and m.slot.coach.id = p.author.id))
            """)
    Optional<Poll> findVisible(
            @Param("pollId") UUID pollId, @Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    Optional<Poll> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
