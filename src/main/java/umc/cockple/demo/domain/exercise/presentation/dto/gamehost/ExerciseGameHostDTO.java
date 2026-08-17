package umc.cockple.demo.domain.exercise.presentation.dto.gamehost;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class ExerciseGameHostDTO {

    @Schema(name = "ExerciseGameHostChangeRequest", description = "게임 진행자 변경 요청")
    public record ChangeRequest(
            @NotNull(message = "게임 진행자 ID는 필수입니다.")
            Long participantId
    ) {
    }

    @Builder
    @Schema(name = "ExerciseGameHostChangeResponse", description = "게임 진행자 변경 응답")
    public record ChangeResponse(
            Long exerciseId,
            Long participantId
    ) {
    }

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
