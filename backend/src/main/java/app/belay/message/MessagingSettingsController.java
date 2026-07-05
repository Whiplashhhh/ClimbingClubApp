package app.belay.message;

import app.belay.auth.UserPrincipal;
import app.belay.message.dto.MessagingSettingsResponse;
import app.belay.message.dto.UpdateMessagingSettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging/settings")
@Tag(name = "messaging", description = "Club messaging settings (general-group rate limit)")
public class MessagingSettingsController {

    private final MessageService messageService;

    public MessagingSettingsController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "The club's messaging settings (general-group rate limit)")
    public MessagingSettingsResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return messageService.getSettings(principal);
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Set the general-group rate limit (admins): unlimited or N messages per window")
    @ApiResponse(responseCode = "400", description = "Provide both a limit and a window, or neither")
    @ApiResponse(responseCode = "403", description = "Only admins may change messaging settings")
    public MessagingSettingsResponse update(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody UpdateMessagingSettingsRequest body) {
        return messageService.updateSettings(principal, body);
    }
}
