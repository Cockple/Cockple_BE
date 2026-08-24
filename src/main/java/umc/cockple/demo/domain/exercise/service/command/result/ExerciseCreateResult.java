package umc.cockple.demo.domain.exercise.service.command.result;

import java.time.LocalDateTime;

public record ExerciseCreateResult(
        Long exerciseId,
        Long gameBoardId,
        LocalDateTime createdAt
) {
}
