package app.belay.friend;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.friend.dto.FriendRequestResponse;
import app.belay.friend.dto.FriendResponse;
import app.belay.organization.OrganizationRepository;
import app.belay.user.AppUser;
import app.belay.user.UserRepository;
import app.belay.user.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public FriendService(
            FriendshipRepository friendshipRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    /** Mes amis (amitiés acceptées), présentés par l'« autre » membre de chaque lien. */
    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(UserPrincipal principal) {
        return friendshipRepository.findAcceptedOf(principal.id()).stream()
                .map(f -> FriendResponse.from(other(f, principal.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> incomingRequests(UserPrincipal principal) {
        return friendshipRepository.findIncomingPending(principal.id()).stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

    @Transactional
    public void sendRequest(UserPrincipal principal, UUID addresseeId) {
        if (addresseeId.equals(principal.id())) {
            throw new ConflictException("Cannot befriend yourself");
        }
        AppUser addressee = userRepository
                .findByIdAndOrganizationId(addresseeId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (addressee.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Member is not active");
        }
        friendshipRepository
                .findBetween(principal.organizationId(), principal.id(), addresseeId)
                .ifPresent(existing -> {
                    throw new ConflictException("A friendship or pending request already exists");
                });
        friendshipRepository.save(new Friendship(
                organizationRepository.getReferenceById(principal.organizationId()),
                userRepository.getReferenceById(principal.id()),
                addressee));
    }

    /** Accepte une demande dont je suis le destinataire. */
    @Transactional
    public void accept(UserPrincipal principal, UUID requesterId) {
        Friendship friendship = friendshipRepository
                .findBetween(principal.organizationId(), principal.id(), requesterId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            return; // idempotent
        }
        // Seul le destinataire de la demande peut l'accepter
        if (!friendship.getAddressee().getId().equals(principal.id())) {
            throw new AccessDeniedException("Only the request addressee can accept it");
        }
        friendship.accept();
    }

    /** Retire un ami, annule une demande envoyée, ou rejette une demande reçue (dans tous les cas). */
    @Transactional
    public void remove(UserPrincipal principal, UUID otherUserId) {
        Friendship friendship = friendshipRepository
                .findBetween(principal.organizationId(), principal.id(), otherUserId)
                .orElseThrow(() -> new NotFoundException("Friendship not found"));
        friendshipRepository.delete(friendship);
    }

    private AppUser other(Friendship friendship, UUID selfId) {
        return friendship.getRequester().getId().equals(selfId) ? friendship.getAddressee() : friendship.getRequester();
    }
}
