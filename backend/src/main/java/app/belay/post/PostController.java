package app.belay.post;

import app.belay.auth.UserPrincipal;
import app.belay.post.dto.CreatePostRequest;
import app.belay.post.dto.FeedPageResponse;
import app.belay.post.dto.FeedPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    @Operation(summary = "Aggregated feed of the caller — active pinned posts (session cancellations) first")
    public FeedPageResponse feed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.feed(principal, page, size);
    }

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Publish a post (ORG audience: admins; COACH_STUDENTS: coaches) with up to 4 optional images",
            description = "Images are content-sniffed (JPEG/PNG/WebP), max 5 MB each")
    @ApiResponse(responseCode = "400", description = "Too many images or a file is not a supported image")
    @ApiResponse(responseCode = "403", description = "Role not allowed to publish to the requested audience")
    public FeedPostResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("meta") CreatePostRequest meta,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return postService.create(principal, meta, images);
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a post (author or admins)")
    @ApiResponse(responseCode = "404", description = "Post not found in the caller's organization")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID postId) {
        postService.delete(principal, postId);
    }
}
