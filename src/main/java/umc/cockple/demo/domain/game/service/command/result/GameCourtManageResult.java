package umc.cockple.demo.domain.game.service.command.result;

import java.util.List;

/**
 * 코트 관리 후의 최종 코트 목록
 */
public record GameCourtManageResult(
        Long gameBoardId,
        List<CourtResult> courts
) {
    public record CourtResult(
            Long courtId,
            int courtNo,
            String courtName
    ) {
    }
}
