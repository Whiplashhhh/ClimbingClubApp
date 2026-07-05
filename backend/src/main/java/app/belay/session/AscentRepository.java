package app.belay.session;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AscentRepository extends JpaRepository<Ascent, UUID> {

    @Query("""
            select a from Ascent a
            join fetch a.route
            left join fetch a.belayerUser
            where a.session.id in :sessionIds
            order by a.createdAt asc
            """)
    List<Ascent> findAllBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

    Optional<Ascent> findByIdAndSessionId(UUID id, UUID sessionId);
}
