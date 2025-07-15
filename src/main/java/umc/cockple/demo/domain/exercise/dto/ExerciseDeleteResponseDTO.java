package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

@Builder
public record ExerciseDeleteResponseDTO(
        Long deletedExerciseId
) {
}
