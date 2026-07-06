package app.belay.account;

import app.belay.account.dto.AccountExportResponse;
import app.belay.account.dto.MessageExport;
import app.belay.account.dto.PollVoteExport;
import app.belay.account.dto.PostExport;
import app.belay.auth.UserPrincipal;
import app.belay.auth.dto.MeResponse;
import app.belay.common.NotFoundException;
import app.belay.friend.FriendService;
import app.belay.message.MessageRepository;
import app.belay.notification.NotificationRepository;
import app.belay.notification.dto.NotificationResponse;
import app.belay.poll.PollVoteRepository;
import app.belay.post.PostRepository;
import app.belay.session.SessionService;
import app.belay.storage.StorageService;
import app.belay.user.AppUser;
import app.belay.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assemble l'export RGPD des données personnelles de l'appelant (lecture seule, scopée à lui). */
@Service
public class AccountService {

    private static final int MAX_NOTIFICATIONS = 500;

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final SessionService sessionService;
    private final FriendService friendService;
    private final PostRepository postRepository;
    private final PollVoteRepository pollVoteRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;

    public AccountService(
            UserRepository userRepository,
            StorageService storageService,
            SessionService sessionService,
            FriendService friendService,
            PostRepository postRepository,
            PollVoteRepository pollVoteRepository,
            MessageRepository messageRepository,
            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.sessionService = sessionService;
        this.friendService = friendService;
        this.postRepository = postRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public AccountExportResponse export(UserPrincipal principal) {
        AppUser user =
                userRepository.findById(principal.id()).orElseThrow(() -> new NotFoundException("User not found"));
        MeResponse profile = MeResponse.from(user, storageService.publicUrlOrNull(user.getAvatarObjectKey()));

        List<PostExport> posts = postRepository.findByAuthorIdOrderByCreatedAtDescIdDesc(principal.id()).stream()
                .map(p -> new PostExport(p.getId(), p.getTitle(), p.getBody(), p.getCreatedAt()))
                .toList();
        List<PollVoteExport> pollVotes = pollVoteRepository.findAllByUserId(principal.id()).stream()
                .map(v -> new PollVoteExport(
                        v.getOption().getPoll().getQuestion(), v.getOption().getLabel()))
                .toList();
        List<MessageExport> messages = messageRepository.findBySenderIdOrderByCreatedAtAsc(principal.id()).stream()
                .map(m -> new MessageExport(m.getConversation().getId(), m.getBody(), m.getCreatedAt()))
                .toList();
        List<NotificationResponse> notifications = notificationRepository
                .findAllByRecipientIdOrderByCreatedAtDescIdDesc(principal.id(), PageRequest.of(0, MAX_NOTIFICATIONS))
                .map(NotificationResponse::from)
                .getContent();

        return new AccountExportResponse(
                profile,
                sessionService.mySessions(principal),
                friendService.listFriends(principal),
                posts,
                pollVotes,
                messages,
                notifications);
    }
}
