package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

public class ExerciseDeleteDTO {

    @Builder
    public record Response(
            Long deletedExerciseId
    ) {
    }
}
