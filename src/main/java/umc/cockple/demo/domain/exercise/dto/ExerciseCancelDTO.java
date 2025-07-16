package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

public class ExerciseCancelDTO {

    @Builder
    public record Response(
            String memberName,
            Integer cancelledParticipationNumber,
            Integer currentParticipants
    ) {
    }
}
