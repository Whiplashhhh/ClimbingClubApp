package app.belay.slot;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.notification.NotificationService;
import app.belay.notification.NotificationType;
import app.belay.organization.OrganizationRepository;
import app.belay.post.Post;
import app.belay.post.PostAudience;
import app.belay.post.PostService;
import app.belay.slot.dto.AddSlotMemberRequest;
import app.belay.slot.dto.CreateSlotChangeRequest;
import app.belay.slot.dto.CreateSlotRequest;
import app.belay.slot.dto.SlotChangeResponse;
import app.belay.slot.dto.SlotMemberResponse;
import app.belay.slot.dto.SlotResponse;
import app.belay.slot.dto.UpdateSlotRequest;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import app.belay.user.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FR_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final SlotRepository slotRepository;
    private final SlotMembershipRepository membershipRepository;
    private final SlotChangeRepository changeRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final PostService postService;

    public SlotService(
            SlotRepository slotRepository,
            SlotMembershipRepository membershipRepository,
            SlotChangeRepository changeRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            NotificationService notificationService,
            PostService postService) {
        this.slotRepository = slotRepository;
        this.membershipRepository = membershipRepository;
        this.changeRepository = changeRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.notificationService = notificationService;
        this.postService = postService;
    }

    /** Planning du club, trié par jour puis heure, avec le groupe et les séances modifiées à venir. */
    @Transactional(readOnly = true)
    public List<SlotResponse> listSlots(UserPrincipal principal) {
        Map<UUID, List<SlotMemberResponse>> membersBySlot =
                membershipRepository.findAllByOrganizationId(principal.organizationId()).stream()
                        .collect(Collectors.groupingBy(
                                m -> m.getSlot().getId(),
                                Collectors.mapping(m -> SlotMemberResponse.from(m.getUser()), Collectors.toList())));
        Map<UUID, List<SlotChangeResponse>> changesBySlot =
                changeRepository.findUpcomingByOrganizationId(principal.organizationId(), LocalDate.now()).stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getSlot().getId(),
                                Collectors.mapping(SlotChangeResponse::from, Collectors.toList())));
        return slotRepository.findAllByOrganizationId(principal.organizationId()).stream()
                .sorted(Comparator.comparing(Slot::getDayOfWeek).thenComparing(Slot::getStartTime))
                .map(slot -> SlotResponse.from(
                        slot,
                        membersBySlot.getOrDefault(slot.getId(), List.of()),
                        changesBySlot.getOrDefault(slot.getId(), List.of())))
                .toList();
    }

    @Transactional
    public SlotResponse createSlot(UserPrincipal principal, CreateSlotRequest request) {
        UUID coachId = request.coachId() == null ? principal.id() : request.coachId();
        // Un moniteur ne crée que ses propres créneaux ; un admin peut cibler n'importe quel éligible
        if (!isAdmin(principal) && !coachId.equals(principal.id())) {
            throw new AccessDeniedException("Coaches can only create their own slots");
        }
        AppUser coach = userRepository
                .findByIdAndOrganizationId(coachId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Coach not found"));
        requireCoachEligible(coach);
        Slot slot = slotRepository.save(new Slot(
                organizationRepository.getReferenceById(principal.organizationId()),
                coach,
                request.name(),
                request.dayOfWeek(),
                request.startTime(),
                request.durationMinutes()));
        return SlotResponse.from(slot, List.of(), List.of());
    }

    @Transactional
    public SlotResponse updateSlot(UserPrincipal principal, UUID slotId, UpdateSlotRequest request) {
        Slot slot = findManagedSlot(principal, slotId);
        slot.setName(request.name());
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setDurationMinutes(request.durationMinutes());
        return toResponse(slot);
    }

    @Transactional
    public void deleteSlot(UserPrincipal principal, UUID slotId) {
        Slot slot = findManagedSlot(principal, slotId);
        changeRepository.deleteAll(changeRepository.findAllBySlotId(slotId));
        membershipRepository.deleteAll(membershipRepository.findAllBySlotId(slotId));
        slotRepository.delete(slot);
    }

    @Transactional
    public SlotResponse addMember(UserPrincipal principal, UUID slotId, AddSlotMemberRequest request) {
        Slot slot = findManagedSlot(principal, slotId);
        AppUser member = userRepository
                .findByIdAndOrganizationId(request.userId(), principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Member is not active");
        }
        if (membershipRepository.existsBySlotIdAndUserId(slotId, member.getId())) {
            throw new ConflictException("Member is already in this slot");
        }
        membershipRepository.save(
                new SlotMembership(organizationRepository.getReferenceById(principal.organizationId()), slot, member));
        return toResponse(slot);
    }

    @Transactional
    public SlotResponse removeMember(UserPrincipal principal, UUID slotId, UUID userId) {
        Slot slot = findManagedSlot(principal, slotId);
        SlotMembership membership = membershipRepository
                .findBySlotIdAndUserId(slotId, userId)
                .orElseThrow(() -> new NotFoundException("Member is not in this slot"));
        membershipRepository.delete(membership);
        return toResponse(slot);
    }

    /** Annule ou décale UNE séance (à une date donnée) et notifie le groupe du créneau. */
    @Transactional
    public SlotResponse createChange(UserPrincipal principal, UUID slotId, CreateSlotChangeRequest request) {
        Slot slot = findManagedSlot(principal, slotId);
        if (request.date().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("The session date is in the past");
        }
        if (request.date().getDayOfWeek() != slot.getDayOfWeek()) {
            throw new IllegalArgumentException("The date does not fall on the slot's weekday");
        }
        boolean moved = request.action() == SlotChangeAction.MOVED;
        if (moved == (request.newStartTime() == null)) {
            throw new IllegalArgumentException("newStartTime is required when MOVED, and only then");
        }
        if (changeRepository.existsBySlotIdAndDate(slotId, request.date())) {
            throw new ConflictException("This session is already cancelled or moved");
        }
        SlotChange change = changeRepository.save(new SlotChange(
                organizationRepository.getReferenceById(principal.organizationId()),
                slot,
                request.date(),
                request.action(),
                request.newStartTime(),
                request.note(),
                userRepository.getReferenceById(principal.id())));
        notifyGroup(principal, slot, change);
        publishToFeed(slot, change);
        return toResponse(slot);
    }

    @Transactional
    public SlotResponse deleteChange(UserPrincipal principal, UUID slotId, UUID changeId) {
        Slot slot = findManagedSlot(principal, slotId);
        SlotChange change = changeRepository
                .findByIdAndSlotId(changeId, slotId)
                .orElseThrow(() -> new NotFoundException("Change not found"));
        // Séance rétablie → le post automatique disparaît du fil
        if (change.getPost() != null) {
            postService.deleteSystemPost(change.getPost());
        }
        changeRepository.delete(change);
        return toResponse(slot);
    }

    /** Destinataires : le groupe + le moniteur, sauf l'auteur de l'action. Message en français (A-004). */
    private void notifyGroup(UserPrincipal actor, Slot slot, SlotChange change) {
        Set<AppUser> recipients = membershipRepository.findAllBySlotId(slot.getId()).stream()
                .map(SlotMembership::getUser)
                .collect(Collectors.toCollection(HashSet::new));
        recipients.add(slot.getCoach());
        recipients.removeIf(user -> user.getId().equals(actor.id()));

        String message = changeHeadline(slot, change);
        if (change.getNote() != null && !change.getNote().isBlank()) {
            message += " — " + change.getNote();
        }
        NotificationType type = change.getAction() == SlotChangeAction.CANCELLED
                ? NotificationType.SLOT_CANCELLED
                : NotificationType.SLOT_MOVED;
        notificationService.notifyAll(slot.getOrganization(), recipients, type, message);
    }

    /**
     * L'annulation apparaît aussi dans le fil des élèves : post « important » publié au nom du
     * moniteur du créneau (l'audience COACH_STUDENTS est résolue via ses créneaux), épinglé en
     * tête du fil jusqu'à la fin du jour de la séance, retiré si la séance est rétablie.
     */
    private void publishToFeed(Slot slot, SlotChange change) {
        Instant pinnedUntil =
                change.getDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Post post = postService.createSystemPost(
                slot.getCoach(),
                PostAudience.COACH_STUDENTS,
                changeHeadline(slot, change),
                change.getNote(),
                true,
                pinnedUntil);
        change.setPost(post);
    }

    private String changeHeadline(Slot slot, SlotChange change) {
        String when = FR_DATE.format(change.getDate());
        return change.getAction() == SlotChangeAction.CANCELLED
                ? "Séance « %s » du %s annulée".formatted(slot.getName(), when)
                : "Séance « %s » du %s décalée à %s"
                        .formatted(slot.getName(), when, FR_TIME.format(change.getNewStartTime()));
    }

    /** Scope tenancy (hors org → 404) puis autorisation : admins, ou le moniteur du créneau. */
    private Slot findManagedSlot(UserPrincipal principal, UUID slotId) {
        Slot slot = slotRepository
                .findByIdAndOrganizationId(slotId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Slot not found"));
        if (!isAdmin(principal) && !slot.getCoach().getId().equals(principal.id())) {
            throw new AccessDeniedException("Only admins or the slot's coach can manage this slot");
        }
        return slot;
    }

    /** Le moniteur d'un créneau ne peut pas être un simple membre (A-009). */
    private void requireCoachEligible(AppUser coach) {
        if (coach.getStatus() != UserStatus.ACTIVE || coach.getRole() == Role.MEMBER) {
            throw new ConflictException("The slot coach must be an active coach or admin");
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.role() == Role.OWNER || principal.role() == Role.ADMIN;
    }

    private SlotResponse toResponse(Slot slot) {
        List<SlotMemberResponse> members = membershipRepository.findAllBySlotId(slot.getId()).stream()
                .map(m -> SlotMemberResponse.from(m.getUser()))
                .toList();
        List<SlotChangeResponse> changes =
                changeRepository
                        .findAllBySlotIdAndDateGreaterThanEqualOrderByDateAsc(slot.getId(), LocalDate.now())
                        .stream()
                        .map(SlotChangeResponse::from)
                        .toList();
        return SlotResponse.from(slot, members, changes);
    }
}
