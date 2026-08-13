package umc.cockple.demo.domain.exercise.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExerciseUpdatedEvent(
        Long exerciseId,
        Long partyId,
        String partyName,
        String imageKey,
        LocalDate exerciseDate,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public ExerciseUpdatedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }

    public static ExerciseUpdatedEvent updated(
            Long exerciseId,
            Long partyId,
            String partyName,
            String imageKey,
            LocalDate exerciseDate,
            List<Long> recipientMemberIds
    ) {
        return new ExerciseUpdatedEvent(
                exerciseId, partyId, partyName, imageKey, exerciseDate,
                recipientMemberIds, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
