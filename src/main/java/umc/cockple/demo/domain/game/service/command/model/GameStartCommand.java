package umc.cockple.demo.domain.game.service.command.model;

/**
 * 게임 시작 내부 command 모델
 *
 * @param gameBoardId 게임판 ID
 * @param gameId      시작할 대기(WAITING) 게임 ID
 * @param courtId     게임을 올릴 목적지 코트 ID
 */
public record GameStartCommand(
        Long gameBoardId,
        Long gameId,
        Long courtId
) {
}
