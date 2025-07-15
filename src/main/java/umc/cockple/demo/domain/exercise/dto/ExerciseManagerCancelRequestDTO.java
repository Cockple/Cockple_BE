package umc.cockple.demo.domain.exercise.dto;

import jakarta.validation.constraints.NotNull;

public record ExerciseManagerCancelRequestDTO(
        @NotNull
        Boolean isGuest
) {
}
