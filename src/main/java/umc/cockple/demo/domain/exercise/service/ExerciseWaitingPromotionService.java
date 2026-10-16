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

    public void notifyPromoted(Exercise exercise, Set<ParticipantKey> previousWaiting) {
        if (previousWaiting.isEmpty()) {
            return;
        }
        Party party = exercise.getParty();
        sortedParticipants(exercise).stream()
                .limit(exercise.getMaxCapacity())
                .filter(participant -> previousWaiting.contains(participant.key()))
                .forEach(participant -> eventPublisher.publishEvent(
                        ExerciseAttendanceChangedEvent.changed(
                                exercise.getId(), party.getId(), party.getPartyName(),
                                party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null,
                                exercise.getDate(),
                                participant.key().guest() ? null : participant.key().id(),
                                List.of(participant.recipientMemberId()),
                                participant.guestName())));
    }

    public record ParticipantKey(boolean guest, Long id) {}

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
