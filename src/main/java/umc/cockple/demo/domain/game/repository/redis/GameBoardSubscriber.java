package umc.cockple.demo.domain.game.repository.redis;

/**
 * 게임판을 구독 중인 하나의 세션. 같은 회원이 여러 세션으로 구독할 수 있으므로
 * 구독 단위는 (memberId, sessionId)다. 발행 시 memberId로 세션을 찾고 sessionId로 그 세션만 겨냥한다.
 */
public record GameBoardSubscriber(
        Long memberId,
        String sessionId
) {
    private static final String DELIMITER = ":";

    public String toToken() {
        return memberId + DELIMITER + sessionId;
    }

    public static GameBoardSubscriber fromToken(String token) {
        int idx = token.indexOf(DELIMITER);
        Long memberId = Long.parseLong(token.substring(0, idx));
        String sessionId = token.substring(idx + 1);
        return new GameBoardSubscriber(memberId, sessionId);
    }
}
