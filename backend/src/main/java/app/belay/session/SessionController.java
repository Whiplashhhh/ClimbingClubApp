package app.belay.session;

import app.belay.auth.UserPrincipal;
import app.belay.session.dto.CreateAscentRequest;
import app.belay.session.dto.CreateSessionRequest;
import app.belay.session.dto.SessionResponse;
import app.belay.session.dto.UpdateSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "sessions", description = "Climbing sessions and ascents (tenant-scoped, owner-managed)")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/mine")
    @Operation(summary = "The caller's own sessions with their ascents, newest first")
    public List<SessionResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return sessionService.mySessions(principal);
    }

    @GetMapping("/club")
    @Operation(summary = "Club activity: the caller's sessions plus other members' CLUB-visible sessions")
    public List<SessionResponse> club(@AuthenticationPrincipal UserPrincipal principal) {
        return sessionService.clubActivity(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a session")
    public SessionResponse start(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreateSessionRequest body) {
        return sessionService.start(principal, body);
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "Update a session's note and visibility (owner only)")
    @ApiResponse(responseCode = "404", description = "Session not found in the caller's organization")
    public SessionResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionRequest body) {
        return sessionService.update(principal, sessionId, body);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a session and its ascents (owner only)")
    @ApiResponse(responseCode = "404", description = "Session not found in the caller's organization")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID sessionId) {
        sessionService.delete(principal, sessionId);
    }

    @PostMapping("/{sessionId}/ascents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an ascent to a session (owner only)")
    @ApiResponse(responseCode = "404", description = "Session or route not found in the caller's organization")
    public SessionResponse addAscent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreateAscentRequest body) {
        return sessionService.addAscent(principal, sessionId, body);
    }

    @DeleteMapping("/{sessionId}/ascents/{ascentId}")
    @Operation(summary = "Remove an ascent from a session (owner only)")
    @ApiResponse(responseCode = "404", description = "Session or ascent not found")
    public SessionResponse removeAscent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID ascentId) {
        return sessionService.removeAscent(principal, sessionId, ascentId);
    }
}
