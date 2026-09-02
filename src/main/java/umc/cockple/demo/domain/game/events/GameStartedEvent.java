package umc.cockple.demo.domain.game.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 대기 게임이 코트에 배치되어 시작되었음을 알리는 이벤트
 *
 * @param gameBoardId        시작된 게임이 속한 게임판
 * @param partyId            게임판이 속한 모임 ID
 * @param partyName          알림 제목으로 사용할 모임 이름
 * @param imageKey           알림 이미지 키
 * @param courtName          게임이 배치된 코트 이름
 * @param recipientMemberIds 알림 수신 대상 회원 ID
 */
public record GameStartedEvent(
        Long gameBoardId,
        Long partyId,
        String partyName,
        String imageKey,
        String courtName,
        List<Long> recipientMemberIds,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public GameStartedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }

    public static GameStartedEvent started(
            Long gameBoardId,
            Long partyId,
            String partyName,
            String imageKey,
            String courtName,
            List<Long> recipientMemberIds
    ) {
        return new GameStartedEvent(
                gameBoardId, partyId, partyName, imageKey, courtName,
                recipientMemberIds, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
