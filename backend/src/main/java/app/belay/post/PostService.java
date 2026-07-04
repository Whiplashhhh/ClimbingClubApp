package app.belay.post;

import app.belay.auth.UserPrincipal;
import app.belay.common.NotFoundException;
import app.belay.organization.OrganizationRepository;
import app.belay.post.dto.CreatePostRequest;
import app.belay.post.dto.CreatePosterRequest;
import app.belay.post.dto.FeedPageResponse;
import app.belay.post.dto.FeedPostResponse;
import app.belay.storage.StorageService;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostService {

    static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final StorageService storageService;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            StorageService storageService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.storageService = storageService;
    }

    @Transactional
    public FeedPostResponse create(UserPrincipal principal, CreatePostRequest request) {
        if (request.type() == PostType.POSTER) {
            throw new IllegalArgumentException("Poster posts must be created via POST /api/posts/poster");
        }
        requireCanPublish(principal, request.audience());
        Post post = save(principal, request.type(), request.audience(), request.title(), request.body(), null);
        return FeedPostResponse.from(post, null);
    }

    @Transactional
    public FeedPostResponse createPoster(UserPrincipal principal, CreatePosterRequest request, MultipartFile image) {
        requireCanPublish(principal, request.audience());
        byte[] content;
        try {
            content = image.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
        // Clé scopée par organisation + regénérée : le nom de fichier client n'est jamais utilisé
        String objectKey = storageService.storeImage(content, "posts/" + principal.organizationId());
        Post post = save(principal, PostType.POSTER, request.audience(), request.title(), request.body(), objectKey);
        return FeedPostResponse.from(post, storageService.presignGet(objectKey));
    }

    @Transactional(readOnly = true)
    public FeedPageResponse feed(UserPrincipal principal, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Slice<Post> slice =
                postRepository.findFeed(principal.organizationId(), principal.id(), PageRequest.of(safePage, safeSize));
        List<FeedPostResponse> items = slice.getContent().stream()
                .map(post -> FeedPostResponse.from(
                        post,
                        post.getImageObjectKey() == null ? null : storageService.presignGet(post.getImageObjectKey())))
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
        String objectKey = post.getImageObjectKey();
        postRepository.delete(post);
        if (objectKey != null) {
            storageService.delete(objectKey);
        }
    }

    /**
     * Publication générée par l'application (ex. annulation de séance) au nom d'un auteur donné —
     * pas de contrôle de rôle : la légitimité est vérifiée par le domaine appelant.
     */
    @Transactional
    public Post createSystemPost(AppUser author, PostType type, PostAudience audience, String title, String body) {
        return postRepository.save(new Post(author.getOrganization(), author, type, audience, title, body, null));
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

    private Post save(
            UserPrincipal principal,
            PostType type,
            PostAudience audience,
            String title,
            String body,
            String imageObjectKey) {
        Post post = new Post(
                organizationRepository.getReferenceById(principal.organizationId()),
                userRepository.getReferenceById(principal.id()),
                type,
                audience,
                title,
                body,
                imageObjectKey);
        return postRepository.save(post);
    }
}
