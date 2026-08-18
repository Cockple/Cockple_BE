package umc.cockple.demo.domain.game.events;

/**
 * REST 명단 관리로 게임판 명단이 변경되었음을 알리는 이벤트.
 * 트랜잭션 커밋 후 명단과 게임판 최신 상태를 구독자에게 전파하는 데 사용한다.
 *
 * @param gameBoardId   변경된 게임판
 * @param actorMemberId REST 요청자
 * @param includeBoardSnapshot 명단과 함께 게임판 snapshot도 전파할지 여부
 */
public record GameBoardMembersChangedEvent(
        Long gameBoardId,
        Long actorMemberId,
        boolean includeBoardSnapshot
) {
    public static GameBoardMembersChangedEvent membersAndBoard(Long gameBoardId, Long actorMemberId) {
        return new GameBoardMembersChangedEvent(gameBoardId, actorMemberId, true);
    }

    public static GameBoardMembersChangedEvent membersOnly(Long gameBoardId, Long actorMemberId) {
        return new GameBoardMembersChangedEvent(gameBoardId, actorMemberId, false);
    }
}
