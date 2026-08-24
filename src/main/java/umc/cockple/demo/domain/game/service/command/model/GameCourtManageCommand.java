package umc.cockple.demo.domain.game.service.command.model;

import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record GameCourtManageCommand(
        Long gameBoardId,
        List<CourtCommand> courts
) {
    private static final int MIN_COURT_COUNT = 1;
    private static final int MAX_COURT_COUNT = 10;

    public GameCourtManageCommand {
        validateGameBoardId(gameBoardId);
        validateNoDuplicateCourtIds(courts);
        validateCourtCount(courts);
    }

    private static void validateGameBoardId(Long gameBoardId) {
        if (gameBoardId == null) {
            throw new GameException(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }
    }

    /**
     * PUT 전체 교체 방식이므로 요청 목록이 곧 관리 후 최종 코트 수다.
     * 기획상 코트는 최소 1개, 최대 10개까지만 유지할 수 있다.
     */
    private static void validateCourtCount(List<CourtCommand> courts) {
        int count = courts == null ? 0 : courts.size();
        if (count < MIN_COURT_COUNT || count > MAX_COURT_COUNT) {
            throw new GameException(GameErrorCode.INVALID_COURT_COUNT);
        }
    }

    /**
     * 같은 courtId가 두 번 이상 들어오면(ex. courtId 10이 중복) 400을 던진다.
     * 중복을 허용하면 같은 코트를 두 번 upsert 하며 courtNo가 꼬이므로 경계에서 차단한다.
     */
    private static void validateNoDuplicateCourtIds(List<CourtCommand> courts) {
        if (courts == null) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        boolean hasDuplicate = courts.stream()
                .map(CourtCommand::courtId)
                .filter(Objects::nonNull)
                .anyMatch(courtId -> !seen.add(courtId));
        if (hasDuplicate) {
            throw new GameException(GameErrorCode.DUPLICATE_COURT_ID);
        }
    }

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
