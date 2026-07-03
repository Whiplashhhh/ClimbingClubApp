package app.belay.member;

import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import app.belay.user.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final UserRepository userRepository;

    public MemberService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AppUser> listActiveMembers(UUID organizationId) {
        return userRepository.findAllByOrganizationIdAndStatusOrderByDisplayNameAsc(organizationId, UserStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<AppUser> listPendingMembers(UUID organizationId) {
        return userRepository.findAllByOrganizationIdAndStatusOrderByDisplayNameAsc(organizationId, UserStatus.PENDING);
    }

    @Transactional
    public AppUser approve(UUID organizationId, UUID memberId) {
        AppUser member = findInOrganization(organizationId, memberId);
        if (member.getStatus() != UserStatus.PENDING) {
            throw new ConflictException("Member is not pending approval");
        }
        member.setStatus(UserStatus.ACTIVE);
        return member;
    }

    @Transactional
    public AppUser changeRole(UUID organizationId, UUID actingUserId, UUID memberId, Role newRole) {
        if (newRole == Role.OWNER) {
            throw new IllegalArgumentException("Ownership transfer is not supported yet");
        }
        if (actingUserId.equals(memberId)) {
            throw new ConflictException("Cannot change your own role");
        }
        AppUser member = findInOrganization(organizationId, memberId);
        if (member.getRole() == Role.OWNER) {
            throw new ConflictException("The owner role cannot be changed");
        }
        member.setRole(newRole);
        return member;
    }

    /** Scope tenancy : une ressource d'une autre organisation est introuvable (404), pas 403. */
    private AppUser findInOrganization(UUID organizationId, UUID memberId) {
        return userRepository
                .findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
    }
}
