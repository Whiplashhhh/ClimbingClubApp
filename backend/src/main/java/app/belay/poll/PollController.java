package app.belay.poll;

import app.belay.auth.UserPrincipal;
import app.belay.poll.dto.CastVoteRequest;
import app.belay.poll.dto.CreatePollRequest;
import app.belay.poll.dto.PollResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/polls")
@Tag(name = "polls", description = "Polls sharing the feed audience model (tenant-scoped)")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping
    @Operation(summary = "Polls visible to the caller (same audience rules as the feed), newest first")
    public List<PollResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return pollService.list(principal);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a poll (ORG audience: admins; COACH_STUDENTS: coaches)")
    @ApiResponse(responseCode = "400", description = "Fewer than 2 options or a blank option")
    @ApiResponse(responseCode = "403", description = "Role not allowed to publish to the requested audience")
    public PollResponse create(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreatePollRequest body) {
        return pollService.create(principal, body);
    }

    @PostMapping("/{pollId}/vote")
    @Operation(summary = "Cast or change the caller's vote on a visible, open poll")
    @ApiResponse(responseCode = "404", description = "Poll or option not found / not visible")
    @ApiResponse(responseCode = "409", description = "Poll is closed")
    public PollResponse vote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID pollId,
            @Valid @RequestBody CastVoteRequest body) {
        return pollService.vote(principal, pollId, body.optionId());
    }

    @DeleteMapping("/{pollId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a poll (author or admins)")
    @ApiResponse(responseCode = "404", description = "Poll not found in the caller's organization")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID pollId) {
        pollService.delete(principal, pollId);
    }
}
