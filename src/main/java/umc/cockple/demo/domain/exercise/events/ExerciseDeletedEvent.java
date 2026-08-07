package umc.cockple.demo.domain.exercise.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExerciseDeletedEvent(
        Long exerciseId,
        Long partyId,
        String partyName,
        String imageKey,
        LocalDate exerciseDate,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt
) {
    public ExerciseDeletedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }

    public static ExerciseDeletedEvent deleted(
            Long exerciseId,
            Long partyId,
            String partyName,
            String imageKey,
            LocalDate exerciseDate,
            List<Long> recipientMemberIds
    ) {
        return new ExerciseDeletedEvent(
                exerciseId, partyId, partyName, imageKey, exerciseDate,
                recipientMemberIds, LocalDateTime.now()
        );
    }
}
