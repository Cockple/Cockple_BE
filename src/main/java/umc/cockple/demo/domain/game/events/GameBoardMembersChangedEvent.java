package umc.cockple.demo.domain.game.events;

/**
 * 명단 정보, 운동 참가자, 활성 게임 상태 변경으로
 * 게임판 명단 projection이 바뀌었음을 알리는 이벤트.
 * 트랜잭션 커밋 후 필요한 snapshot을 구독자에게 전파하는 데 사용한다.
 *
 * @param gameBoardId   변경된 게임판
 * @param actorMemberId 변경 요청자
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
