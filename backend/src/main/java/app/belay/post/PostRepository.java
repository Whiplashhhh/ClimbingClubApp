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
     * ORG = tout le monde ; COACH_STUDENTS = les élèves du moniteur auteur (SlotMembership,
     * Phase 3 — A-005) — en attendant, seul l'auteur voit ses publications COACH_STUDENTS.
     */
    @Query("""
            select p from Post p
            join fetch p.author
            where p.organization.id = :organizationId
              and (p.audience = app.belay.post.PostAudience.ORG or p.author.id = :userId)
            order by p.createdAt desc, p.id desc
            """)
    Slice<Post> findFeed(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, Pageable pageable);

    Optional<Post> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
