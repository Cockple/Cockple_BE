package umc.cockple.demo.domain.notification.command;

import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.enums.NotificationSource;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxEventType;

import java.util.UUID;

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
        NotificationDestination destination,
        NotificationLegacyCompatibility legacyCompatibility,
        UUID eventId,
        NotificationOutboxEventType eventType
) {
    public NotificationRequest(
            NotificationSource source,
            Long recipientMemberId,
            String title,
            String content,
            String imageKey,
            String data,
            NotificationDestination destination,
            NotificationLegacyCompatibility legacyCompatibility
    ) {
        this(source, recipientMemberId, title, content, imageKey, data, destination,
                legacyCompatibility, null, null);
    }
}
