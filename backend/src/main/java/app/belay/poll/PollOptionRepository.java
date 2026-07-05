package app.belay.poll;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    List<PollOption> findAllByPollIdOrderByPositionAsc(UUID pollId);

    List<PollOption> findAllByPollIdInOrderByPollIdAscPositionAsc(List<UUID> pollIds);

    Optional<PollOption> findByIdAndPollId(UUID id, UUID pollId);
}
