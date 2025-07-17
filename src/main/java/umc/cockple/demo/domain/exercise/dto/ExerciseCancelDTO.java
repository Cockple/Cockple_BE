package umc.cockple.demo.domain.exercise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class ExerciseCancelDTO {

    public record ByManagerRequest(
            @NotNull
            Boolean isGuest
    ) {
    }

    @Builder
    public record Response(
            String memberName,
            Integer currentParticipants
    ) {
    }
}
