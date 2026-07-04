package app.belay.slot;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.organization.OrganizationRepository;
import app.belay.slot.dto.AddSlotMemberRequest;
import app.belay.slot.dto.CreateSlotRequest;
import app.belay.slot.dto.SlotMemberResponse;
import app.belay.slot.dto.SlotResponse;
import app.belay.slot.dto.UpdateSlotRequest;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import app.belay.user.UserStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final SlotMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public SlotService(
            SlotRepository slotRepository,
            SlotMembershipRepository membershipRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository) {
        this.slotRepository = slotRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    /** Planning du club, trié par jour puis heure, avec le groupe de chaque créneau. */
    @Transactional(readOnly = true)
    public List<SlotResponse> listSlots(UserPrincipal principal) {
        Map<UUID, List<SlotMemberResponse>> membersBySlot =
                membershipRepository.findAllByOrganizationId(principal.organizationId()).stream()
                        .collect(Collectors.groupingBy(
                                m -> m.getSlot().getId(),
                                Collectors.mapping(m -> SlotMemberResponse.from(m.getUser()), Collectors.toList())));
        return slotRepository.findAllByOrganizationId(principal.organizationId()).stream()
                .sorted(Comparator.comparing(Slot::getDayOfWeek).thenComparing(Slot::getStartTime))
                .map(slot -> SlotResponse.from(slot, membersBySlot.getOrDefault(slot.getId(), List.of())))
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
        return SlotResponse.from(slot, List.of());
    }

    @Transactional
    public SlotResponse updateSlot(UserPrincipal principal, UUID slotId, UpdateSlotRequest request) {
        Slot slot = findManagedSlot(principal, slotId);
        slot.setName(request.name());
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setDurationMinutes(request.durationMinutes());
        return SlotResponse.from(slot, membersOf(slotId));
    }

    @Transactional
    public void deleteSlot(UserPrincipal principal, UUID slotId) {
        Slot slot = findManagedSlot(principal, slotId);
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
        return SlotResponse.from(slot, membersOf(slotId));
    }

    @Transactional
    public SlotResponse removeMember(UserPrincipal principal, UUID slotId, UUID userId) {
        Slot slot = findManagedSlot(principal, slotId);
        SlotMembership membership = membershipRepository
                .findBySlotIdAndUserId(slotId, userId)
                .orElseThrow(() -> new NotFoundException("Member is not in this slot"));
        membershipRepository.delete(membership);
        return SlotResponse.from(slot, membersOf(slotId));
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

    private List<SlotMemberResponse> membersOf(UUID slotId) {
        return membershipRepository.findAllBySlotId(slotId).stream()
                .map(m -> SlotMemberResponse.from(m.getUser()))
                .toList();
    }
}
