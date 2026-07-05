package app.belay.message;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.message.dto.ConversationResponse;
import app.belay.message.dto.MessageResponse;
import app.belay.notification.NotificationService;
import app.belay.notification.NotificationType;
import app.belay.organization.OrganizationRepository;
import app.belay.user.AppUser;
import app.belay.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Messagerie privée : un fil relie un moniteur et l'un de ses élèves (relation dérivée d'un
 * créneau, A-005). Seuls les deux participants voient et écrivent ; l'envoi notifie le destinataire.
 */
@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;

    public MessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UserPrincipal principal) {
        List<Conversation> conversations =
                conversationRepository.findAllForUser(principal.organizationId(), principal.id());
        if (conversations.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = conversations.stream().map(Conversation::getId).toList();
        Map<UUID, String> previewByConversation = messageRepository.findLatestPerConversation(ids).stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), Message::getBody, (a, b) -> a));
        Map<UUID, Long> unreadByConversation = messageRepository.countUnreadByConversation(principal.id(), ids).stream()
                .collect(Collectors.toMap(
                        MessageRepository.UnreadCount::getConversationId, MessageRepository.UnreadCount::getTotal));
        return conversations.stream()
                .map(c -> ConversationResponse.from(
                        c,
                        principal.id(),
                        previewByConversation.get(c.getId()),
                        unreadByConversation.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /** Démarre (ou récupère) le fil avec un membre éligible : moniteur ↔ l'un de ses élèves. */
    @Transactional
    public ConversationResponse startConversation(UserPrincipal principal, UUID otherUserId) {
        if (otherUserId.equals(principal.id())) {
            throw new ConflictException("Cannot message yourself");
        }
        AppUser other = userRepository
                .findByIdAndOrganizationId(otherUserId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        UUID coachId;
        UUID studentId;
        if (conversationRepository.existsCoachStudentRelation(
                principal.organizationId(), principal.id(), otherUserId)) {
            coachId = principal.id(); // je suis le moniteur de l'autre
            studentId = otherUserId;
        } else if (conversationRepository.existsCoachStudentRelation(
                principal.organizationId(), otherUserId, principal.id())) {
            coachId = otherUserId; // l'autre est mon moniteur
            studentId = principal.id();
        } else {
            throw new ConflictException("No coaching relationship with this member");
        }

        Conversation conversation = conversationRepository
                .findByCoachIdAndStudentId(coachId, studentId)
                .orElseGet(() -> conversationRepository.save(new Conversation(
                        organizationRepository.getReferenceById(principal.organizationId()),
                        userRepository.getReferenceById(coachId),
                        userRepository.getReferenceById(studentId))));
        long unread =
                messageRepository.countUnreadByConversation(principal.id(), List.of(conversation.getId())).stream()
                        .findFirst()
                        .map(MessageRepository.UnreadCount::getTotal)
                        .orElse(0L);
        return ConversationResponse.from(conversation, principal.id(), previewOf(conversation.getId()), unread);
    }

    /** Les messages d'un fil dont l'appelant est participant ; l'ouverture marque les reçus comme lus. */
    @Transactional
    public List<MessageResponse> thread(UserPrincipal principal, UUID conversationId) {
        Conversation conversation = requireParticipant(principal, conversationId);
        messageRepository.markThreadRead(conversation.getId(), principal.id(), Instant.now());
        return messageRepository.findThread(conversation.getId()).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse send(UserPrincipal principal, UUID conversationId, String body) {
        Conversation conversation = requireParticipant(principal, conversationId);
        Instant now = Instant.now();
        Message message = messageRepository.save(new Message(
                conversation.getOrganization(), conversation, userRepository.getReferenceById(principal.id()), body));
        conversation.touch(now);
        // Notifie le destinataire (l'autre participant) — message rédigé côté domaine (A-004)
        AppUser recipient = conversation.other(principal.id());
        String senderName = message.getSender().getDisplayName();
        notificationService.notifyAll(
                conversation.getOrganization(),
                List.of(recipient),
                NotificationType.NEW_MESSAGE,
                "Nouveau message de " + senderName);
        return MessageResponse.from(message);
    }

    private Conversation requireParticipant(UserPrincipal principal, UUID conversationId) {
        return conversationRepository
                .findVisible(conversationId, principal.organizationId(), principal.id())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private String previewOf(UUID conversationId) {
        return messageRepository.findLatestPerConversation(List.of(conversationId)).stream()
                .findFirst()
                .map(Message::getBody)
                .orElse(null);
    }
}
