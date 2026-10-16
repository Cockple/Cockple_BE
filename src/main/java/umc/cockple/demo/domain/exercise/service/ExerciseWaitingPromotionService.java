package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.exercise.service.support.calculator.ExerciseParticipantPositionCalculator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseWaitingPromotionService {

    private final ExerciseParticipantPositionCalculator positionCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public Set<ParticipantKey> findWaitingParticipants(Exercise exercise) {
        if (exercise.getNowCapacity() <= exercise.getMaxCapacity()) {
            return Set.of();
        }
        return sortedParticipants(exercise).stream()
                .skip(exercise.getMaxCapacity())
                .map(Participant::key)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<PromotionCandidate> findPromotionCandidate(Exercise exercise, ParticipantKey cancelledParticipant) {
        if (exercise.getNowCapacity() <= exercise.getMaxCapacity()) {
            return Optional.empty();
        }

        List<Participant> participants = sortedParticipants(exercise);
        boolean cancellingConfirmedParticipant = participants.stream()
                .limit(exercise.getMaxCapacity())
                .anyMatch(participant -> participant.key().equals(cancelledParticipant));
        if (!cancellingConfirmedParticipant) {
            return Optional.empty();
        }

        Participant firstWaiting = participants.get(exercise.getMaxCapacity());
        return Optional.of(new PromotionCandidate(
                firstWaiting.key(), firstWaiting.recipientMemberId(), firstWaiting.guestName()));
    }

    public void notifyPromoted(Exercise exercise, PromotionCandidate candidate) {
        Party party = exercise.getParty();
        eventPublisher.publishEvent(ExerciseAttendanceChangedEvent.changed(
                exercise.getId(), party.getId(), party.getPartyName(),
                party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null,
                exercise.getDate(),
                candidate.key().guest() ? null : candidate.key().id(),
                List.of(candidate.recipientMemberId()), candidate.guestName()));
    }

    public record ParticipantKey(boolean guest, Long id) {}

    public record PromotionCandidate(ParticipantKey key, Long recipientMemberId, String guestName) {}

    private List<Participant> sortedParticipants(Exercise exercise) {
        List<Participant> participants = new ArrayList<>();
        exercise.getParticipations().forEach(participation -> participants.add(new Participant(
                new ParticipantKey(false, participation.getMember().getId()),
                participation.getMember().getId(), null, participation.getCreatedAt())));
        exercise.getGuests().forEach(guest -> participants.add(new Participant(
                new ParticipantKey(true, guest.getId()),
                guest.getInviterId(), guest.getGuestName(), guest.getCreatedAt())));
        return positionCalculator.sortByJoinedAt(participants, Participant::joinedAt);
    }

    private record Participant(
            ParticipantKey key,
            Long recipientMemberId,
            String guestName,
            LocalDateTime joinedAt
    ) {}
}
