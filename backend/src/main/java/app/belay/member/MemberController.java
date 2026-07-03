package app.belay.member;

import app.belay.auth.UserPrincipal;
import app.belay.member.dto.MemberResponse;
import app.belay.member.dto.PendingMemberResponse;
import app.belay.member.dto.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@Tag(name = "members", description = "Organization members management (tenant-scoped)")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List active members of the caller's organization")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal UserPrincipal principal) {
        return memberService.listActiveMembers(principal.organizationId()).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "List members awaiting approval (admins only)")
    public List<PendingMemberResponse> listPendingMembers(@AuthenticationPrincipal UserPrincipal principal) {
        return memberService.listPendingMembers(principal.organizationId()).stream()
                .map(PendingMemberResponse::from)
                .toList();
    }

    @PostMapping("/{memberId}/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Approve a pending member (admins only)")
    @ApiResponse(responseCode = "404", description = "Member not found in the caller's organization")
    @ApiResponse(responseCode = "409", description = "Member is not pending")
    public MemberResponse approve(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID memberId) {
        return MemberResponse.from(memberService.approve(principal.organizationId(), memberId));
    }

    @PatchMapping("/{memberId}/role")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Change a member's role (admins only, OWNER excluded)")
    @ApiResponse(responseCode = "404", description = "Member not found in the caller's organization")
    public MemberResponse changeRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateRoleRequest body) {
        return MemberResponse.from(
                memberService.changeRole(principal.organizationId(), principal.id(), memberId, body.role()));
    }
}
