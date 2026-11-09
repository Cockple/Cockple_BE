package umc.cockple.demo.domain.game.service.command.result;

import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;

/**
 * 게임 대기 생성 결과
 *
 * @param gameId 생성된 게임 ID
 * @param board  생성 트랜잭션 안에서 조립한 최신 보드 스냅샷
 */
public record GameCreateResult(
        Long gameId,
        GameBoardResult board
) {
}
