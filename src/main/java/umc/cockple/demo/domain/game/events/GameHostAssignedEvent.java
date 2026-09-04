package umc.cockple.demo.domain.game.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 운동(게임판)의 게임 진행자로 특정 회원이 지정되었음을 알리는 이벤트
 *
 * @param gameBoardId       진행자가 지정된 게임판 (알림 destination resourceId)
 * @param partyId           게임판이 속한 모임 ID (레거시 호환용)
 * @param partyName         알림 제목으로 사용할 모임 이름
 * @param imageKey          알림 이미지 키 (모임 이미지, 없으면 null)
 * @param recipientMemberId 알림 수신 대상 회원 ID (새로 지정된 진행자 본인)
 */
public record GameHostAssignedEvent(
        Long gameBoardId,
        Long partyId,
        String partyName,
        String imageKey,
        Long recipientMemberId,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static GameHostAssignedEvent assigned(
            Long gameBoardId,
            Long partyId,
            String partyName,
            String imageKey,
            Long recipientMemberId
    ) {
        return new GameHostAssignedEvent(
                gameBoardId, partyId, partyName, imageKey, recipientMemberId,
                LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
