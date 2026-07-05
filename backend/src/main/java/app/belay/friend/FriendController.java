package app.belay.friend;

import app.belay.auth.UserPrincipal;
import app.belay.friend.dto.FriendRequestResponse;
import app.belay.friend.dto.FriendResponse;
import app.belay.friend.dto.SendFriendRequestRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
@Tag(name = "friends", description = "Friend graph within the organization")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    @Operation(summary = "The caller's accepted friends")
    public List<FriendResponse> friends(@AuthenticationPrincipal UserPrincipal principal) {
        return friendService.listFriends(principal);
    }

    @GetMapping("/requests/incoming")
    @Operation(summary = "Pending friend requests addressed to the caller")
    public List<FriendRequestResponse> incoming(@AuthenticationPrincipal UserPrincipal principal) {
        return friendService.incomingRequests(principal);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a friend request to an active member of the club")
    @ApiResponse(responseCode = "404", description = "Member not found in the caller's organization")
    @ApiResponse(responseCode = "409", description = "Self-request, inactive member, or existing link")
    public void send(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody SendFriendRequestRequest body) {
        friendService.sendRequest(principal, body.addresseeId());
    }

    @PostMapping("/{requesterId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Accept an incoming friend request (the caller is the addressee)")
    @ApiResponse(responseCode = "404", description = "Request not found")
    public void accept(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID requesterId) {
        friendService.accept(principal, requesterId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a friend, cancel a sent request, or reject a received one")
    @ApiResponse(responseCode = "404", description = "Friendship not found")
    public void remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID userId) {
        friendService.remove(principal, userId);
    }
}
