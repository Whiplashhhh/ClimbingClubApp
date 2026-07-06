package app.belay.poll;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    Optional<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);

    List<PollVote> findAllByUserIdAndPollIdIn(UUID userId, List<UUID> pollIds);

    /** Tous les votes d'un utilisateur (export RGPD), option et sondage chargés. */
    @Query("""
            select v from PollVote v
            join fetch v.option o
            join fetch o.poll
            where v.user.id = :userId
            """)
    List<PollVote> findAllByUserId(@Param("userId") UUID userId);

    /** Nombre de voix par option, pour les sondages donnés (agrégat des résultats). */
    @Query("""
            select v.option.id as optionId, count(v.id) as total
            from PollVote v
            where v.poll.id in :pollIds
            group by v.option.id
            """)
    List<OptionCount> countByOptionForPolls(@Param("pollIds") List<UUID> pollIds);

    /** Projection : (option, nombre de voix). */
    interface OptionCount {
        UUID getOptionId();

        long getTotal();
    }
}
