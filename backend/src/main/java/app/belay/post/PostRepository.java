package app.belay.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /** Posts rédigés par un utilisateur (export RGPD). */
    List<Post> findByAuthorIdOrderByCreatedAtDescIdDesc(UUID authorId);

    /**
     * Fil d'un utilisateur : publications de SON organisation dont il fait partie de l'audience.
     * ORG = tout le monde ; COACH_STUDENTS = visibles par l'auteur et par les élèves du moniteur
     * auteur, c'est-à-dire les membres d'un de ses créneaux (SlotMembership — A-005).
     * Les posts épinglés encore actifs (annulations de séances à venir) passent devant.
     */
    @Query("""
            select p from Post p
            join fetch p.author
            where p.organization.id = :organizationId
              and (p.audience = app.belay.post.PostAudience.ORG
                   or p.author.id = :userId
                   or exists (select 1 from SlotMembership m
                              where m.user.id = :userId and m.slot.coach.id = p.author.id))
            order by case when p.pinnedUntil is not null and p.pinnedUntil > CURRENT_TIMESTAMP then 0 else 1 end,
                     p.createdAt desc, p.id desc
            """)
    Slice<Post> findFeed(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, Pageable pageable);

    Optional<Post> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
