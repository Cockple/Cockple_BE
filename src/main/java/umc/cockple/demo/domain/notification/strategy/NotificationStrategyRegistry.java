package umc.cockple.demo.domain.notification.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.notification.command.NotificationRequest;

import java.util.List;

/**
 * 이벤트에 맞는 알림 변환 전략을 선택
 */
@Component
@RequiredArgsConstructor
public class NotificationStrategyRegistry {

    private final List<NotificationEventStrategy> strategies;

    public NotificationEventStrategy find(Object event) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(event))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "알림 변환 전략을 찾을 수 없습니다. eventType="
                                + (event == null ? "null" : event.getClass().getName())));
    }

    public List<NotificationRequest> convert(Object event) {
        return find(event).convert(event);
    }
}
