package umc.cockple.demo.domain.game.service.query.result;

import java.util.List;

/**
 * 게임 중복 체크 결과
 */
public record GameDuplicateCheckResult(
        List<PairView> pairs
) {
    /**
     * @param count            그날(이 게임판) 완료된 게임 중 두 멤버가 함께 참여한 횟수
     * @param playedInLastGame 직전(가장 최근) 완료 게임에 두 멤버가 함께 있었는지
     */
    public record PairView(
            Long memberIdA,
            Long memberIdB,
            int count,
            boolean playedInLastGame
    ) {
    }
}
