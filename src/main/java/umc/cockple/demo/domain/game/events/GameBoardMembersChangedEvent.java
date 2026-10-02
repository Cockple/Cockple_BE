package umc.cockple.demo.domain.game.events;

import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;

import java.util.Objects;

/**
 * 명단 정보, 운동 참가자, 활성 게임 상태 변경으로
 * 게임판 명단 projection이 바뀌었음을 알리는 이벤트.
 * 트랜잭션 커밋 후 필요한 snapshot을 구독자에게 전파하는 데 사용
 *
 * @param gameBoardId   변경된 게임판
 * @param actorMemberId 변경 요청자
 * @param includeBoardSnapshot 명단과 함께 게임판 snapshot도 전파할지 여부
 * @param boardSnapshot 변경 트랜잭션 안에서 이미 조립한 게임판 snapshot. 있으면 커밋 후 재조회 없이 그대로 전파하고,
 *                      null 이면 리스너가 커밋 후 게임판을 조회
 */
public record GameBoardMembersChangedEvent(
        Long gameBoardId,
        Long actorMemberId,
        boolean includeBoardSnapshot,
        GameBoardResult boardSnapshot
) {
    public static GameBoardMembersChangedEvent membersAndBoard(Long gameBoardId, Long actorMemberId) {
        return new GameBoardMembersChangedEvent(gameBoardId, actorMemberId, true, null);
    }

    public static GameBoardMembersChangedEvent membersWithBoard(
            Long gameBoardId, Long actorMemberId, GameBoardResult boardSnapshot) {
        Objects.requireNonNull(boardSnapshot, "boardSnapshot은 null일 수 없습니다.");
        return new GameBoardMembersChangedEvent(gameBoardId, actorMemberId, true, boardSnapshot);
    }

    public static GameBoardMembersChangedEvent membersOnly(Long gameBoardId, Long actorMemberId) {
        return new GameBoardMembersChangedEvent(gameBoardId, actorMemberId, false, null);
    }
}
