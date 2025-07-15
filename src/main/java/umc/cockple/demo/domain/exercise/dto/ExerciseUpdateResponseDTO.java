package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ExerciseUpdateResponseDTO(
        Long exerciseId,
        LocalDateTime updatedAt
) {
}
