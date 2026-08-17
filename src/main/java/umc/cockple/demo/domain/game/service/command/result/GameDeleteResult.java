package umc.cockple.demo.domain.game.service.command.result;

import umc.cockple.demo.global.enums.Level;

import java.util.List;

/**
 * 게임 취소/대기 삭제 결과
 *
 * @param gameId  삭제된 게임 ID
 * @param players restore=true 일 때만 채워지는 복원용 플레이어 목록
 */
public record GameDeleteResult(
        Long gameId,
        List<PlayerView> players
) {
    public record PlayerView(
            Long gameBoardMemberId,
            String name,
            Level level,
            int playerOrder
    ) {
    }
}
