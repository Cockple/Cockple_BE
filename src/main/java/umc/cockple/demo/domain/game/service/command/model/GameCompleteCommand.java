package umc.cockple.demo.domain.game.service.command.model;

/**
 * 게임 완료 내부 command 모델
 *
 * @param gameBoardId 게임판 ID
 * @param gameId      완료할 진행(PLAYING) 게임 ID
 */
public record GameCompleteCommand(
        Long gameBoardId,
        Long gameId
) {
}
