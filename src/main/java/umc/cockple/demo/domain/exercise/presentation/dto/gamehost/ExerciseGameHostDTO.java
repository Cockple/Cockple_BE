package umc.cockple.demo.domain.exercise.presentation.dto.gamehost;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class ExerciseGameHostDTO {

    @Builder
    @Schema(name = "ExerciseGameHostResponse", description = "게임 진행자 후보 목록 응답")
    public record Response(
            Integer totalCount,
            List<Participant> participants
    ) {
    }

    @Builder
    @Schema(name = "ExerciseGameHostParticipant", description = "게임 진행자 후보")
    public record Participant(
            Long participantId,
            String profileImageUrl,
            String partyPosition,
            Boolean isGameHost,
            String name,
            String gender,
            String level,
            LocalDate lastExerciseDate
    ) {
    }
}
