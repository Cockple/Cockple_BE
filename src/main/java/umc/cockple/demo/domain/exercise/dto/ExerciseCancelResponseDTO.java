package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

@Builder
public record ExerciseCancelResponseDTO(
        String memberName,
        Integer cancelledParticipationNumber,
        Integer currentParticipants
) {
}
