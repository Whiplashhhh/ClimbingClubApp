package app.belay.post;

import app.belay.auth.UserPrincipal;
import app.belay.post.dto.CreatePostRequest;
import app.belay.post.dto.CreatePosterRequest;
import app.belay.post.dto.FeedPageResponse;
import app.belay.post.dto.FeedPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "feed", description = "Home feed: posts with an audience, aggregated per user (tenant-scoped)")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/feed")
    @Operation(summary = "Aggregated feed of the caller: ORG posts plus COACH_STUDENTS posts they can see")
    public FeedPageResponse feed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.feed(principal, page, size);
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish an INFO or CANCELLATION post (ORG audience: admins; COACH_STUDENTS: coaches)")
    @ApiResponse(responseCode = "403", description = "Role not allowed to publish to the requested audience")
    public FeedPostResponse create(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreatePostRequest body) {
        return postService.create(principal, body);
    }

    @PostMapping(value = "/posts/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a POSTER post with an image (content-sniffed JPEG/PNG/WebP, max 5 MB)")
    @ApiResponse(responseCode = "400", description = "File content is not a supported image")
    @ApiResponse(responseCode = "403", description = "Role not allowed to publish to the requested audience")
    public FeedPostResponse createPoster(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("meta") CreatePosterRequest meta,
            @RequestPart("image") MultipartFile image) {
        return postService.createPoster(principal, meta, image);
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a post (author or admins)")
    @ApiResponse(responseCode = "404", description = "Post not found in the caller's organization")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID postId) {
        postService.delete(principal, postId);
    }
}
