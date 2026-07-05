package app.belay.session;

import app.belay.auth.UserPrincipal;
import app.belay.common.NotFoundException;
import app.belay.organization.OrganizationRepository;
import app.belay.route.Route;
import app.belay.route.RouteRepository;
import app.belay.session.dto.AscentResponse;
import app.belay.session.dto.CreateAscentRequest;
import app.belay.session.dto.CreateSessionRequest;
import app.belay.session.dto.SessionResponse;
import app.belay.session.dto.UpdateSessionRequest;
import app.belay.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final ClimbingSessionRepository sessionRepository;
    private final AscentRepository ascentRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public SessionService(
            ClimbingSessionRepository sessionRepository,
            AscentRepository ascentRepository,
            RouteRepository routeRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository) {
        this.sessionRepository = sessionRepository;
        this.ascentRepository = ascentRepository;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public SessionResponse start(UserPrincipal principal, CreateSessionRequest request) {
        ClimbingSession session = sessionRepository.save(new ClimbingSession(
                organizationRepository.getReferenceById(principal.organizationId()),
                userRepository.getReferenceById(principal.id()),
                request.startedAt() == null ? Instant.now() : request.startedAt(),
                request.note(),
                request.visibility()));
        return SessionResponse.from(session, List.of());
    }

    @Transactional
    public SessionResponse update(UserPrincipal principal, UUID sessionId, UpdateSessionRequest request) {
        ClimbingSession session = findOwnedSession(principal, sessionId);
        session.setNote(request.note());
        session.setVisibility(request.visibility());
        return toResponse(session);
    }

    @Transactional
    public void delete(UserPrincipal principal, UUID sessionId) {
        ClimbingSession session = findOwnedSession(principal, sessionId);
        ascentRepository.deleteAll(ascentRepository.findAllBySessionIds(List.of(sessionId)));
        sessionRepository.delete(session);
    }

    @Transactional
    public SessionResponse addAscent(UserPrincipal principal, UUID sessionId, CreateAscentRequest request) {
        ClimbingSession session = findOwnedSession(principal, sessionId);
        Route route = routeRepository
                .findByIdAndOrganizationId(request.routeId(), principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Route not found"));
        ascentRepository.save(new Ascent(
                organizationRepository.getReferenceById(principal.organizationId()),
                session,
                route,
                request.rating(),
                request.topHold(),
                request.durationSeconds(),
                request.belayerName()));
        return toResponse(session);
    }

    @Transactional
    public SessionResponse removeAscent(UserPrincipal principal, UUID sessionId, UUID ascentId) {
        ClimbingSession session = findOwnedSession(principal, sessionId);
        Ascent ascent = ascentRepository
                .findByIdAndSessionId(ascentId, sessionId)
                .orElseThrow(() -> new NotFoundException("Ascent not found"));
        ascentRepository.delete(ascent);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> mySessions(UserPrincipal principal) {
        return withAscents(sessionRepository.findMine(principal.id()));
    }

    /** Fil d'activité : mes séances + les séances CLUB des autres membres. */
    @Transactional(readOnly = true)
    public List<SessionResponse> clubActivity(UserPrincipal principal) {
        return withAscents(sessionRepository.findClubActivity(principal.organizationId(), principal.id()));
    }

    /** Une séance n'est modifiable que par son auteur ; hors org → 404 (existence masquée). */
    private ClimbingSession findOwnedSession(UserPrincipal principal, UUID sessionId) {
        ClimbingSession session = sessionRepository
                .findByIdAndOrganizationId(sessionId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!session.getUser().getId().equals(principal.id())) {
            throw new AccessDeniedException("Only the session owner can manage it");
        }
        return session;
    }

    private SessionResponse toResponse(ClimbingSession session) {
        List<AscentResponse> ascents = ascentRepository.findAllBySessionIds(List.of(session.getId())).stream()
                .map(AscentResponse::from)
                .toList();
        return SessionResponse.from(session, ascents);
    }

    private List<SessionResponse> withAscents(List<ClimbingSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<AscentResponse>> ascentsBySession =
                ascentRepository
                        .findAllBySessionIds(
                                sessions.stream().map(ClimbingSession::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(
                                a -> a.getSession().getId(),
                                Collectors.mapping(AscentResponse::from, Collectors.toList())));
        return sessions.stream()
                .map(s -> SessionResponse.from(s, ascentsBySession.getOrDefault(s.getId(), List.of())))
                .toList();
    }
}
