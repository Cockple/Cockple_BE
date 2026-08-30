package umc.cockple.demo.domain.notification.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.enums.NotificationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationStrategyRegistryTest {

    private final NotificationEventStrategy partyStrategy = new TestPartyStrategy();
    private final NotificationStrategyRegistry registry =
            new NotificationStrategyRegistry(List.of(partyStrategy));

    @Test
    @DisplayName("이벤트 타입에 맞는 전략을 선택한다")
    void findsStrategyForEventType() {
        TestPartyEvent event = new TestPartyEvent(1L);

        assertThat(registry.find(event)).isSameAs(partyStrategy);
    }

    @Test
    @DisplayName("선택된 전략으로 공통 알림 요청을 변환한다")
    void convertsEventToNotificationRequests() {
        List<NotificationRequest> requests = registry.convert(new TestPartyEvent(1L));

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).source()).isEqualTo(NotificationSource.PARTY);
        assertThat(requests.get(0).recipientMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("지원하는 전략이 없으면 명확한 예외를 던진다")
    void throwsWhenStrategyDoesNotSupportEvent() {
        assertThatThrownBy(() -> registry.find(new UnsupportedEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UnsupportedEvent.class.getName());
    }

    private record TestPartyEvent(Long memberId) {
    }

    private record UnsupportedEvent() {
    }

    private static class TestPartyStrategy implements NotificationEventStrategy {

        @Override
        public boolean supports(Object event) {
            return event instanceof TestPartyEvent;
        }

        @Override
        public List<NotificationRequest> convert(Object event) {
            TestPartyEvent partyEvent = (TestPartyEvent) event;
            return List.of(new NotificationRequest(
                    NotificationSource.PARTY,
                    partyEvent.memberId(),
                    "title",
                    "content",
                    null,
                    null,
                    null,
                    null
            ));
        }
    }
}
