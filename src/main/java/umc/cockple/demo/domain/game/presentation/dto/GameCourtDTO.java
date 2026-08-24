package umc.cockple.demo.domain.game.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class GameCourtDTO {

    @Schema(name = "GameCourtManageRequest", description = "게임 코트 관리 요청")
    public record Request(
            @NotNull(message = "코트 목록은 필수입니다. (전체 삭제 시 빈 배열)")
            List<@NotNull(message = "코트 항목은 null일 수 없습니다.") CourtRequest> courts
    ) {
    }

    public record CourtRequest(
            Long courtId,
            String courtName
    ) {
    }

    public record Response(
            Long gameBoardId,
            List<CourtInfo> courts
    ) {
    }

    public record CourtInfo(
            Long courtId,
            int courtNo,
            String courtName
    ) {
    }
}
