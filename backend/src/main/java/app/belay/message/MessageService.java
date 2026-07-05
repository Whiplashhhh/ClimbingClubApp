package app.belay.message;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.message.dto.ConversationResponse;
import app.belay.message.dto.MessageResponse;
import app.belay.notification.NotificationService;
import app.belay.notification.NotificationType;
import app.belay.organization.OrganizationRepository;
import app.belay.slot.SlotRepository;
import app.belay.user.AppUser;
import app.belay.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Messagerie. Trois formes de fils : DIRECT (1:1 moniteur↔élève), SLOT (groupe d'un créneau) et
 * GENERAL (groupe du club). Les groupes sont auto-provisionnés et leur accès est calculé
 * (appartenance au créneau / au club), si bien qu'un nouvel arrivant voit tout l'historique.
 */
@Service
public class MessageService {

    static final String GENERAL_TITLE = "Tout le club";

    private final ConversationRepository conversationRepository;
    private final ConversationReadRepository readRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SlotRepository slotRepository;
    private final NotificationService notificationService;

    public MessageService(
            ConversationRepository conversationRepository,
            ConversationReadRepository readRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            SlotRepository slotRepository,
            NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.readRepository = readRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.slotRepository = slotRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<ConversationResponse> listConversations(UserPrincipal principal) {
        UUID orgId = principal.organizationId();
        UUID me = principal.id();

        // Provisionnement paresseux des groupes visibles par l'utilisateur
        Conversation general = ensureGeneral(orgId);
        List<UUID> slotIds = conversationRepository.findSlotIdsForUser(orgId, me);
        slotIds.forEach(slotId -> ensureSlotGroup(orgId, slotId));

        List<Conversation> conversations = new ArrayList<>(conversationRepository.findDirectForUser(orgId, me));
        if (!slotIds.isEmpty()) {
            conversations.addAll(conversationRepository.findSlotConversations(slotIds));
        }
        conversations.add(general);

        List<UUID> ids = conversations.stream().map(Conversation::getId).toList();
        Map<UUID, String> preview = messageRepository.findLatestPerConversation(ids).stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), Message::getBody, (a, b) -> a));
        Map<UUID, Long> unread = messageRepository.countUnreadByConversation(me, ids).stream()
                .collect(Collectors.toMap(
                        MessageRepository.UnreadCount::getConversationId, MessageRepository.UnreadCount::getTotal));

        return conversations.stream()
                .map(c -> toResponse(c, me, preview.get(c.getId()), unread.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparing(ConversationResponse::lastMessageAt)
                        .reversed())
                .toList();
    }

    /** Démarre (ou récupère) un fil 1:1 avec un membre éligible : moniteur ↔ l'un de ses élèves. */
    @Transactional
    public ConversationResponse startConversation(UserPrincipal principal, UUID otherUserId) {
        if (otherUserId.equals(principal.id())) {
            throw new ConflictException("Cannot message yourself");
        }
        userRepository
                .findByIdAndOrganizationId(otherUserId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        UUID coachId;
        UUID studentId;
        if (conversationRepository.existsCoachStudentRelation(
                principal.organizationId(), principal.id(), otherUserId)) {
            coachId = principal.id();
            studentId = otherUserId;
        } else if (conversationRepository.existsCoachStudentRelation(
                principal.organizationId(), otherUserId, principal.id())) {
            coachId = otherUserId;
            studentId = principal.id();
        } else {
            throw new ConflictException("No coaching relationship with this member");
        }

        Conversation conversation = conversationRepository
                .findByCoachIdAndStudentId(coachId, studentId)
                .orElseGet(() -> conversationRepository.save(Conversation.direct(
                        organizationRepository.getReferenceById(principal.organizationId()),
                        userRepository.getReferenceById(coachId),
                        userRepository.getReferenceById(studentId))));
        return toResponse(
                conversation, principal.id(), previewOf(conversation.getId()), unreadOf(conversation, principal.id()));
    }

    /** Les messages d'un fil accessible à l'appelant ; l'ouverture marque le fil comme lu. */
    @Transactional
    public List<MessageResponse> thread(UserPrincipal principal, UUID conversationId) {
        Conversation conversation = requireAccess(principal, conversationId);
        markRead(conversation, principal.id());
        return messageRepository.findThread(conversation.getId()).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse send(UserPrincipal principal, UUID conversationId, String body) {
        Conversation conversation = requireAccess(principal, conversationId);
        Message message = messageRepository.save(new Message(
                conversation.getOrganization(), conversation, userRepository.getReferenceById(principal.id()), body));
        conversation.touch(Instant.now());
        // Notification in-app réservée aux 1:1 : dans un groupe, la pastille de non-lus suffit
        // (notifier tout le monde à chaque message serait du spam).
        if (conversation.getType() == ConversationType.DIRECT) {
            AppUser recipient = conversation.directOther(principal.id());
            notificationService.notifyAll(
                    conversation.getOrganization(),
                    List.of(recipient),
                    NotificationType.NEW_MESSAGE,
                    "Nouveau message de " + message.getSender().getDisplayName());
        }
        return MessageResponse.from(message);
    }

    // --- Accès & provisionnement ---

    private Conversation requireAccess(UserPrincipal principal, UUID conversationId) {
        Conversation c = conversationRepository
                .findByIdAndOrganizationId(conversationId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        boolean allowed =
                switch (c.getType()) {
                    case DIRECT ->
                        c.getCoach().getId().equals(principal.id())
                                || c.getStudent().getId().equals(principal.id());
                    case SLOT ->
                        conversationRepository.isSlotParticipant(c.getSlot().getId(), principal.id());
                    case GENERAL -> true; // même organisation = membre actif
                };
        if (!allowed) {
            throw new NotFoundException("Conversation not found"); // existence masquée
        }
        return c;
    }

    private Conversation ensureGeneral(UUID orgId) {
        return conversationRepository
                .findByOrganizationIdAndType(orgId, ConversationType.GENERAL)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.general(organizationRepository.getReferenceById(orgId))));
    }

    private void ensureSlotGroup(UUID orgId, UUID slotId) {
        if (conversationRepository.findBySlotId(slotId).isEmpty()) {
            conversationRepository.save(Conversation.forSlot(
                    organizationRepository.getReferenceById(orgId), slotRepository.getReferenceById(slotId)));
        }
    }

    private void markRead(Conversation conversation, UUID userId) {
        Instant now = Instant.now();
        readRepository
                .findByConversationIdAndUserId(conversation.getId(), userId)
                .ifPresentOrElse(
                        read -> read.markReadAt(now),
                        () -> readRepository.save(new ConversationRead(
                                conversation.getOrganization(),
                                conversation,
                                userRepository.getReferenceById(userId),
                                now)));
    }

    private ConversationResponse toResponse(Conversation c, UUID me, String preview, long unread) {
        return switch (c.getType()) {
            case DIRECT -> ConversationResponse.direct(c, me, preview, unread);
            case SLOT -> ConversationResponse.group(c, c.getSlot().getName(), preview, unread);
            case GENERAL -> ConversationResponse.group(c, GENERAL_TITLE, preview, unread);
        };
    }

    private String previewOf(UUID conversationId) {
        return messageRepository.findLatestPerConversation(List.of(conversationId)).stream()
                .findFirst()
                .map(Message::getBody)
                .orElse(null);
    }

    private long unreadOf(Conversation conversation, UUID userId) {
        return messageRepository.countUnreadByConversation(userId, List.of(conversation.getId())).stream()
                .findFirst()
                .map(MessageRepository.UnreadCount::getTotal)
                .orElse(0L);
    }
}
