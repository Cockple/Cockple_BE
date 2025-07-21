package umc.cockple.demo.domain.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class ExerciseCancelDTO {

    @Schema(name = "ExerciseCancelByManagerRequest", description = "매니저 권한에 의한 운동 참여 취소 요청")
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
