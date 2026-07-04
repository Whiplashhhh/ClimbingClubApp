package app.belay.notification;

import app.belay.auth.UserPrincipal;
import app.belay.notification.dto.NotificationPageResponse;
import app.belay.notification.dto.NotificationResponse;
import app.belay.organization.Organization;
import app.belay.user.AppUser;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notifications in-app : domaine générique, les messages sont rédigés par les domaines appelants. */
@Service
public class NotificationService {

    static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyAll(
            Organization organization, Collection<AppUser> recipients, NotificationType type, String message) {
        List<Notification> notifications = recipients.stream()
                .map(recipient -> new Notification(organization, recipient, type, message))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse page(UserPrincipal principal, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Slice<Notification> slice = notificationRepository.findAllByRecipientIdOrderByCreatedAtDescIdDesc(
                principal.id(), PageRequest.of(safePage, safeSize));
        List<NotificationResponse> items =
                slice.getContent().stream().map(NotificationResponse::from).toList();
        long unread = notificationRepository.countByRecipientIdAndReadAtIsNull(principal.id());
        return new NotificationPageResponse(items, safePage, safeSize, slice.hasNext(), unread);
    }

    @Transactional
    public void markAllRead(UserPrincipal principal) {
        notificationRepository.markAllRead(principal.id(), Instant.now());
    }
}
