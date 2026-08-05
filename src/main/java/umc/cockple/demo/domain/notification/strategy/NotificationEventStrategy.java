package umc.cockple.demo.domain.notification.strategy;

import umc.cockple.demo.domain.notification.command.NotificationRequest;

import java.util.List;

/**
 * 도메인 이벤트를 공통 알림 요청으로 변환하는 전략
 */
public interface NotificationEventStrategy {

    /**
     * 이 전략이 처리할 이벤트 타입
     */
    Class<?> eventType();

    default boolean supports(Object event) {
        return event != null && eventType().isInstance(event);
    }

    List<NotificationRequest> convert(Object event);
}
