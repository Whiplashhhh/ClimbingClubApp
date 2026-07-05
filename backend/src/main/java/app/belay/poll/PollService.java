package app.belay.poll;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.organization.OrganizationRepository;
import app.belay.poll.dto.CreatePollRequest;
import app.belay.poll.dto.PollOptionResponse;
import app.belay.poll.dto.PollResponse;
import app.belay.post.PostAudience;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final PollVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public PollService(
            PollRepository pollRepository,
            PollOptionRepository optionRepository,
            PollVoteRepository voteRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository) {
        this.pollRepository = pollRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public PollResponse create(UserPrincipal principal, CreatePollRequest request) {
        requireCanPublish(principal, request.audience());
        Poll poll = pollRepository.save(new Poll(
                organizationRepository.getReferenceById(principal.organizationId()),
                userRepository.getReferenceById(principal.id()),
                request.audience(),
                request.question(),
                request.closesAt()));
        short position = 0;
        for (String label : request.options()) {
            optionRepository.save(new PollOption(poll.getOrganization(), poll, label.strip(), position++));
        }
        // Sondage neuf : aucune voix, aucun choix personnel encore
        List<PollOptionResponse> options = optionRepository.findAllByPollIdOrderByPositionAsc(poll.getId()).stream()
                .map(o -> new PollOptionResponse(o.getId(), o.getLabel(), 0L))
                .toList();
        return PollResponse.from(poll, options, null);
    }

    @Transactional(readOnly = true)
    public List<PollResponse> list(UserPrincipal principal) {
        List<Poll> polls = pollRepository.findAllVisible(principal.organizationId(), principal.id());
        if (polls.isEmpty()) {
            return List.of();
        }
        List<UUID> pollIds = polls.stream().map(Poll::getId).toList();

        Map<UUID, List<PollOption>> optionsByPoll =
                optionRepository.findAllByPollIdInOrderByPollIdAscPositionAsc(pollIds).stream()
                        .collect(Collectors.groupingBy(o -> o.getPoll().getId()));
        Map<UUID, Long> countByOption = voteRepository.countByOptionForPolls(pollIds).stream()
                .collect(Collectors.toMap(
                        PollVoteRepository.OptionCount::getOptionId, PollVoteRepository.OptionCount::getTotal));
        Map<UUID, UUID> myChoiceByPoll = voteRepository.findAllByUserIdAndPollIdIn(principal.id(), pollIds).stream()
                .collect(Collectors.toMap(
                        v -> v.getOption().getPoll().getId(), v -> v.getOption().getId()));

        return polls.stream()
                .map(poll -> {
                    List<PollOptionResponse> options = optionsByPoll.getOrDefault(poll.getId(), List.of()).stream()
                            .map(o -> new PollOptionResponse(
                                    o.getId(), o.getLabel(), countByOption.getOrDefault(o.getId(), 0L)))
                            .toList();
                    return PollResponse.from(poll, options, myChoiceByPoll.get(poll.getId()));
                })
                .toList();
    }

    /** Voter (ou changer de vote) sur un sondage visible et ouvert. */
    @Transactional
    public PollResponse vote(UserPrincipal principal, UUID pollId, UUID optionId) {
        Poll poll = pollRepository
                .findVisible(pollId, principal.organizationId(), principal.id())
                .orElseThrow(() -> new NotFoundException("Poll not found"));
        if (poll.isClosed()) {
            throw new ConflictException("This poll is closed");
        }
        PollOption option = optionRepository
                .findByIdAndPollId(optionId, pollId)
                .orElseThrow(() -> new NotFoundException("Option not found for this poll"));
        voteRepository
                .findByPollIdAndUserId(pollId, principal.id())
                .ifPresentOrElse(
                        existing -> existing.changeOption(option),
                        () -> voteRepository.save(new PollVote(
                                poll.getOrganization(),
                                poll,
                                option,
                                userRepository.getReferenceById(principal.id()))));
        return single(principal, poll);
    }

    /** L'auteur ou un OWNER/ADMIN de l'org peut supprimer ; hors org → 404 (existence masquée). */
    @Transactional
    public void delete(UserPrincipal principal, UUID pollId) {
        Poll poll = pollRepository
                .findByIdAndOrganizationId(pollId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Poll not found"));
        boolean isAuthor = poll.getAuthor().getId().equals(principal.id());
        boolean isAdmin = principal.role() == Role.OWNER || principal.role() == Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("Only the author or an admin can delete a poll");
        }
        pollRepository.delete(poll); // options et votes partent en cascade (ON DELETE CASCADE)
    }

    private PollResponse single(UserPrincipal principal, Poll poll) {
        List<UUID> ids = List.of(poll.getId());
        Map<UUID, Long> countByOption = voteRepository.countByOptionForPolls(ids).stream()
                .collect(Collectors.toMap(
                        PollVoteRepository.OptionCount::getOptionId, PollVoteRepository.OptionCount::getTotal));
        UUID myChoice = voteRepository
                .findByPollIdAndUserId(poll.getId(), principal.id())
                .map(v -> v.getOption().getId())
                .orElse(null);
        List<PollOptionResponse> options = optionRepository.findAllByPollIdOrderByPositionAsc(poll.getId()).stream()
                .map(o -> new PollOptionResponse(o.getId(), o.getLabel(), countByOption.getOrDefault(o.getId(), 0L)))
                .toList();
        return PollResponse.from(poll, options, myChoice);
    }

    /** Matrice de publication (identique au fil) : ORG → OWNER/ADMIN ; COACH_STUDENTS → COACH. */
    private void requireCanPublish(UserPrincipal principal, PostAudience audience) {
        boolean allowed =
                switch (audience) {
                    case ORG -> principal.role() == Role.OWNER || principal.role() == Role.ADMIN;
                    case COACH_STUDENTS -> principal.role() == Role.COACH;
                };
        if (!allowed) {
            throw new AccessDeniedException("Role " + principal.role() + " cannot publish to audience " + audience);
        }
    }
}
