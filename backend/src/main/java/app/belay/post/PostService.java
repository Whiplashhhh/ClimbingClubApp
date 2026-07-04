package app.belay.post;

import app.belay.auth.UserPrincipal;
import app.belay.common.NotFoundException;
import app.belay.organization.OrganizationRepository;
import app.belay.post.dto.CreatePostRequest;
import app.belay.post.dto.FeedPageResponse;
import app.belay.post.dto.FeedPostResponse;
import app.belay.storage.StorageService;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostService {

    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_IMAGES = 4;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final StorageService storageService;

    public PostService(
            PostRepository postRepository,
            PostImageRepository postImageRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            StorageService storageService) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.storageService = storageService;
    }

    @Transactional
    public FeedPostResponse create(UserPrincipal principal, CreatePostRequest request, List<MultipartFile> images) {
        requireCanPublish(principal, request.audience());
        if (images != null && images.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("A post can carry at most " + MAX_IMAGES + " images");
        }
        Post post = postRepository.save(new Post(
                organizationRepository.getReferenceById(principal.organizationId()),
                userRepository.getReferenceById(principal.id()),
                request.audience(),
                request.title(),
                request.body(),
                false,
                null));
        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            int position = 0;
            for (MultipartFile image : images) {
                // Clé scopée par organisation + regénérée : le nom de fichier client n'est jamais utilisé
                String objectKey = storageService.storeImage(bytesOf(image), "posts/" + principal.organizationId());
                postImageRepository.save(new PostImage(post.getOrganization(), post, objectKey, position++));
                imageUrls.add(storageService.presignGet(objectKey));
            }
        }
        return FeedPostResponse.from(post, imageUrls);
    }

    @Transactional(readOnly = true)
    public FeedPageResponse feed(UserPrincipal principal, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Slice<Post> slice =
                postRepository.findFeed(principal.organizationId(), principal.id(), PageRequest.of(safePage, safeSize));
        List<UUID> postIds = slice.getContent().stream().map(Post::getId).toList();
        Map<UUID, List<String>> urlsByPost = postIds.isEmpty()
                ? Map.of()
                : postImageRepository.findAllByPostIdInOrderByPostIdAscPositionAsc(postIds).stream()
                        .collect(Collectors.groupingBy(
                                image -> image.getPost().getId(),
                                Collectors.mapping(
                                        image -> storageService.presignGet(image.getObjectKey()),
                                        Collectors.toList())));
        List<FeedPostResponse> items = slice.getContent().stream()
                .map(post -> FeedPostResponse.from(post, urlsByPost.getOrDefault(post.getId(), List.of())))
                .toList();
        return new FeedPageResponse(items, safePage, safeSize, slice.hasNext());
    }

    /** L'auteur ou un OWNER/ADMIN de l'org peut supprimer ; hors org → 404 (existence masquée). */
    @Transactional
    public void delete(UserPrincipal principal, UUID postId) {
        Post post = postRepository
                .findByIdAndOrganizationId(postId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Post not found"));
        boolean isAuthor = post.getAuthor().getId().equals(principal.id());
        boolean isAdmin = principal.role() == Role.OWNER || principal.role() == Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("Only the author or an admin can delete a post");
        }
        List<String> objectKeys = postImageRepository.findAllByPostIdOrderByPositionAsc(postId).stream()
                .map(PostImage::getObjectKey)
                .toList();
        // Les lignes post_image partent avec le post (ON DELETE CASCADE)
        postRepository.delete(post);
        objectKeys.forEach(storageService::delete);
    }

    /**
     * Publication générée par l'application (ex. annulation de séance) au nom d'un auteur donné —
     * pas de contrôle de rôle : la légitimité est vérifiée par le domaine appelant.
     */
    @Transactional
    public Post createSystemPost(
            AppUser author, PostAudience audience, String title, String body, boolean important, Instant pinnedUntil) {
        return postRepository.save(
                new Post(author.getOrganization(), author, audience, title, body, important, pinnedUntil));
    }

    /** Suppression d'un post généré par l'application (jamais d'image associée). */
    @Transactional
    public void deleteSystemPost(Post post) {
        postRepository.delete(post);
    }

    /** Matrice de permissions du fil : ORG → OWNER/ADMIN ; COACH_STUDENTS → COACH. */
    private void requireCanPublish(UserPrincipal principal, PostAudience audience) {
        boolean allowed =
                switch (audience) {
                    case ORG -> principal.role() == Role.OWNER || principal.role() == Role.ADMIN;
                    case COACH_STUDENTS -> principal.role() == Role.COACH;
                };
        if (!allowed) {
            throw new AccessDeniedException("Role " + principal.role() + " cannot publish to audience " + audience);
        }
    }

    private byte[] bytesOf(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
