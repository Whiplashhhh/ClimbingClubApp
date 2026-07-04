package app.belay.slot;

import app.belay.auth.UserPrincipal;
import app.belay.slot.dto.AddSlotMemberRequest;
import app.belay.slot.dto.CreateSlotRequest;
import app.belay.slot.dto.SlotResponse;
import app.belay.slot.dto.UpdateSlotRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/slots")
@Tag(name = "slots", description = "Weekly recurring training slots and their member groups (tenant-scoped)")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    @Operation(summary = "List the club's slots with coach and group members")
    public List<SlotResponse> listSlots(@AuthenticationPrincipal UserPrincipal principal) {
        return slotService.listSlots(principal);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a weekly slot (coaches: own slots only; admins: any eligible coach)")
    @ApiResponse(responseCode = "403", description = "Coach tried to create a slot for someone else")
    @ApiResponse(responseCode = "409", description = "Target coach is not an active coach or admin")
    public SlotResponse createSlot(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreateSlotRequest body) {
        return slotService.createSlot(principal, body);
    }

    @PutMapping("/{slotId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @Operation(summary = "Update a slot (admins or the slot's coach)")
    @ApiResponse(responseCode = "404", description = "Slot not found in the caller's organization")
    public SlotResponse updateSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateSlotRequest body) {
        return slotService.updateSlot(principal, slotId, body);
    }

    @DeleteMapping("/{slotId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a slot and its memberships (admins or the slot's coach)")
    @ApiResponse(responseCode = "404", description = "Slot not found in the caller's organization")
    public void deleteSlot(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID slotId) {
        slotService.deleteSlot(principal, slotId);
    }

    @PostMapping("/{slotId}/members")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an active member to the slot's group (admins or the slot's coach)")
    @ApiResponse(responseCode = "404", description = "Slot or member not found in the caller's organization")
    @ApiResponse(responseCode = "409", description = "Member is pending/disabled or already in the slot")
    public SlotResponse addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID slotId,
            @Valid @RequestBody AddSlotMemberRequest body) {
        return slotService.addMember(principal, slotId, body);
    }

    @DeleteMapping("/{slotId}/members/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @Operation(summary = "Remove a member from the slot's group (admins or the slot's coach)")
    @ApiResponse(responseCode = "404", description = "Slot or membership not found")
    public SlotResponse removeMember(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID slotId, @PathVariable UUID userId) {
        return slotService.removeMember(principal, slotId, userId);
    }
}
