package umc.cockple.demo.domain.exercise.service.command.result;

import java.time.LocalDateTime;

public record ExerciseJoinResult(
        Long participantId,
        LocalDateTime joinedAt,
        Integer currentParticipants
) {
}
