package umc.cockple.demo.domain.game.events;

/**
 * 코트 관리(REST)로 게임판의 코트 구성이 바뀌었음을 알리는 이벤트.
 * 트랜잭션 커밋 후 같은 게임판을 구독 중인 다른 사용자에게 보드 갱신을 브로드캐스트하는 데 쓰인다.
 *
 * @param gameBoardId   변경된 게임판
 * @param actorMemberId 요청자(브로드캐스트 대상에서 제외 — 이미 REST 응답으로 최신 상태를 받았다)
 */
public record GameCourtsManagedEvent(
        Long gameBoardId,
        Long actorMemberId
) {
}
