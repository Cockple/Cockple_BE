package umc.cockple.demo.domain.notification.command;

import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.enums.NotificationSource;

/**
 * 도메인 이벤트를 알림 생성에 필요한 정보로 변환한 공통 요청 모델
 */
public record NotificationRequest(
        NotificationSource source,
        Long recipientMemberId,
        String title,
        String content,
        String imageKey,
        String data,
        NotificationDestination destination
) {
}
