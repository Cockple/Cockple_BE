package umc.cockple.demo.domain.exercise.service.command.result;

import java.time.LocalDateTime;

public record ExerciseUpdateResult(
        Long exerciseId,
        LocalDateTime updatedAt
) {
}
