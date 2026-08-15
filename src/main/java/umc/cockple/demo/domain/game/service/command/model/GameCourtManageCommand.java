package umc.cockple.demo.domain.game.service.command.model;

import java.util.List;

public record GameCourtManageCommand(
        Long gameBoardId,
        List<CourtCommand> courts
) {
    /**
     * @param courtId   기존 코트면 값 존재(유지 대상), null 이면 신규 생성 대상
     * @param courtName 생략(null/blank) 시 서비스에서 courtNo 기반 기본값 부여
     */
    public record CourtCommand(
            Long courtId,
            String courtName
    ) {
    }
}
