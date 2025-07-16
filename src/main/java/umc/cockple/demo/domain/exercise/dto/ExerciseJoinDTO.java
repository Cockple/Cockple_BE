package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDateTime;

public class ExerciseJoinDTO {

    @Builder
    public record Response(
            Long participantId,
            Integer participantNumber,
            LocalDateTime joinedAt,
            Integer currentParticipants
    ) {
    }
}
