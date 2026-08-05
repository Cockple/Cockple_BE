package umc.cockple.demo.domain.exercise.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExerciseUpdatedEvent(
        Long exerciseId,
        Long partyId,
        String partyName,
        String imageKey,
        LocalDate exerciseDate,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt
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
                recipientMemberIds, LocalDateTime.now()
        );
    }
}
