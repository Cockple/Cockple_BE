package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ExerciseJoinResponseDTO(
        Long participantId,
        Integer participantNumber,
        LocalDateTime joinedAt,
        Integer currentParticipants
) {
}
