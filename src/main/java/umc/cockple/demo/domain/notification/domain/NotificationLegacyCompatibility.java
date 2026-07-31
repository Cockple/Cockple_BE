package umc.cockple.demo.domain.notification.domain;

import umc.cockple.demo.domain.notification.enums.NotificationType;

/**
 * V2 알림을 V1 클라이언트가 읽을 수 있도록 유지하는 내부 호환 정보다.
 * 외부 API 응답 모델이나 V2 destination 모델에 포함하지 않는다.
 */
public record NotificationLegacyCompatibility(
        Long partyId,
        NotificationType type
) {
}
