package umc.cockple.demo.domain.game.service.command.model;

/**
 * 게임 취소/대기 삭제 내부 command 모델
 *
 * @param gameBoardId 게임판 ID
 * @param gameId      삭제할 게임 ID (대기/진행 게임)
 * @param restore     true 이면 삭제된 게임의 플레이어 목록을 복원용으로 반환한다.
 */
public record GameDeleteCommand(
        Long gameBoardId,
        Long gameId,
        boolean restore
) {
}
