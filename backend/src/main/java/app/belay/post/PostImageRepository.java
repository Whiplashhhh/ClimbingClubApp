package app.belay.post;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

    List<PostImage> findAllByPostIdInOrderByPostIdAscPositionAsc(Collection<UUID> postIds);

    List<PostImage> findAllByPostIdOrderByPositionAsc(UUID postId);
}
