package umc.cockple.demo.domain.exercise.dto;

import java.time.LocalDateTime;

public record ExerciseCreateResponseDTO(
    Long exerciseId,
    LocalDateTime createdAt
) {
}
