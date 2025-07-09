package umc.cockple.demo.domain.exercise.dto;

import java.time.LocalDateTime;

public record ExerciseJoinResponseDTO(
        Long participantId,
        LocalDateTime joinedAt,
        Integer currentParticipants
) {
}
