package umc.cockple.demo.domain.exercise.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExerciseJoinedEvent(
        Long exerciseId,
        Long partyId,
        String partyName,
        String imageKey,
        LocalDate exerciseDate,
        String participantName,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public ExerciseJoinedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }

    public static ExerciseJoinedEvent joined(
            Long exerciseId,
            Long partyId,
            String partyName,
            String imageKey,
            LocalDate exerciseDate,
            String participantName,
            List<Long> recipientMemberIds
    ) {
        return new ExerciseJoinedEvent(
                exerciseId, partyId, partyName, imageKey, exerciseDate,
                participantName, recipientMemberIds, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
