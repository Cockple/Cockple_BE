package umc.cockple.demo.domain.game.service.command.model;

/**
 * 대기열 이동 내부 command 모델
 *
 * @param gameBoardId 게임판 ID
 * @param gameId      대기열로 되돌릴 진행(PLAYING) 게임 ID
 */
public record GameToWaitingCommand(
        Long gameBoardId,
        Long gameId
) {
}
