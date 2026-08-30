package umc.cockple.demo.domain.exercise.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExerciseAttendanceChangedEvent(
        Long exerciseId,
        Long partyId,
        String partyName,
        String imageKey,
        LocalDate exerciseDate,
        Long subjectMemberId,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt,
        UUID eventId
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
                subjectMemberId, recipientMemberIds, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
