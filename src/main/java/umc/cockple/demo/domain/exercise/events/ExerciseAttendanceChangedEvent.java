package umc.cockple.demo.domain.exercise.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExerciseAttendanceChangedEvent(
        Long exerciseId,
        Long partyId,
        String partyName,
        String imageKey,
        LocalDate exerciseDate,
        Long subjectMemberId,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt
) {
    public ExerciseAttendanceChangedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }

    public static ExerciseAttendanceChangedEvent changed(
            Long exerciseId,
            Long partyId,
            String partyName,
            String imageKey,
            LocalDate exerciseDate,
            Long subjectMemberId,
            List<Long> recipientMemberIds
    ) {
        return new ExerciseAttendanceChangedEvent(
                exerciseId, partyId, partyName, imageKey, exerciseDate,
                subjectMemberId, recipientMemberIds, LocalDateTime.now()
        );
    }
}
