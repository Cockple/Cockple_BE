package umc.cockple.demo.domain.exercise.presentation.dto.lifecycle;

import lombok.Builder;

public class ExerciseDeleteDTO {

    @Builder
    public record Response(
            Long deletedExerciseId
    ) {
    }
}
