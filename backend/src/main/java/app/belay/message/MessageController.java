package app.belay.message;

import app.belay.auth.UserPrincipal;
import app.belay.message.dto.ConversationResponse;
import app.belay.message.dto.MessageResponse;
import app.belay.message.dto.SendMessageRequest;
import app.belay.message.dto.StartConversationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "messaging", description = "Private coach↔student conversations (tenant-scoped)")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "The caller's conversations, most recently active first")
    public List<ConversationResponse> conversations(@AuthenticationPrincipal UserPrincipal principal) {
        return messageService.listConversations(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start (or fetch) a conversation with an eligible member — a coach or one of their students")
    @ApiResponse(responseCode = "404", description = "Member not found in the caller's organization")
    @ApiResponse(responseCode = "409", description = "Self-conversation or no coaching relationship")
    public ConversationResponse start(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody StartConversationRequest body) {
        return messageService.startConversation(principal, body.userId());
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Messages of a conversation the caller belongs to (marks received messages as read)")
    @ApiResponse(responseCode = "404", description = "Conversation not found or caller not a participant")
    public List<MessageResponse> messages(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID conversationId) {
        return messageService.thread(principal, conversationId);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a message in a conversation the caller belongs to")
    @ApiResponse(responseCode = "404", description = "Conversation not found or caller not a participant")
    public MessageResponse send(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest body) {
        return messageService.send(principal, conversationId, body.body());
    }
}
