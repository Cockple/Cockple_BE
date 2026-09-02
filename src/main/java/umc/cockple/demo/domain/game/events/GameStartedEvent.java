package umc.cockple.demo.domain.game.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 대기 게임이 코트에 배치되어 시작되었음을 알리는 이벤트.
 * 게임에 배정된 회원(게스트 제외)에게 게임판 입장 알림을 발송하는 데 사용한다.
 *
 * @param gameBoardId        시작된 게임이 속한 게임판 (알림 destination resourceId)
 * @param partyId            게임판이 속한 모임 ID (레거시 호환용)
 * @param partyName          알림 제목으로 사용할 모임 이름
 * @param imageKey           알림 이미지 키 (모임 이미지, 없으면 null)
 * @param courtName          게임이 배치된 코트 이름 (사용자 지정 이름 또는 "N번 코트" 기본값)
 * @param recipientMemberIds 알림 수신 대상 회원 ID (게스트 제외)
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
