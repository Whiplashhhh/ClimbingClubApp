package app.belay.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Fil d'un utilisateur : publications de SON organisation dont il fait partie de l'audience.
     * ORG = tout le monde ; COACH_STUDENTS = visibles par l'auteur et par les élèves du moniteur
     * auteur, c'est-à-dire les membres d'un de ses créneaux (SlotMembership — A-005).
     */
    @Query("""
            select p from Post p
            join fetch p.author
            where p.organization.id = :organizationId
              and (p.audience = app.belay.post.PostAudience.ORG
                   or p.author.id = :userId
                   or exists (select 1 from SlotMembership m
                              where m.user.id = :userId and m.slot.coach.id = p.author.id))
            order by p.createdAt desc, p.id desc
            """)
    Slice<Post> findFeed(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, Pageable pageable);

    Optional<Post> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
