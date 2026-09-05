package umc.cockple.demo.domain.notification.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.events.GameHostAssignedEvent;
import umc.cockple.demo.domain.game.events.GameStartedEvent;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationSource;
import umc.cockple.demo.domain.notification.service.NotificationMessageGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameNotificationStrategyTest {

    // 운영 환경의 자동설정 ObjectMapper와 동일하게 직렬화를 지원하도록 JavaTimeModule을 등록한다.
    private final GameNotificationStrategy strategy =
            new GameNotificationStrategy(
                    new NotificationMessageGenerator(),
                    new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    @DisplayName("게임 시작 이벤트를 지원한다")
    void supportsGameStartedEvent() {
        boolean supports = strategy.supports(
                GameStartedEvent.started(1L, 10L, "모임", "image-key", "1번 코트", List.of(20L)));

        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("게임 시작 알림은 수신자별로 생성되고 게임판 destination을 가진다")
    void convertsGameStartedNotification() {
        List<NotificationRequest> requests = strategy.convert(
                GameStartedEvent.started(1L, 10L, "모임", "image-key", "화이팅코트", List.of(20L, 30L)));

        assertThat(requests).extracting(NotificationRequest::recipientMemberId)
                .containsExactly(20L, 30L);
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.source()).isEqualTo(NotificationSource.GAME);
            assertThat(request.title()).isEqualTo("모임");
            assertThat(request.destination().resourceType())
                    .isEqualTo(NotificationResourceType.GAME_BOARD);
            assertThat(request.destination().resourceId()).isEqualTo(1L);
            assertThat(request.destination().action()).isEqualTo(NotificationAction.VIEW);
        });
    }

    @Test
    @DisplayName("게임 시작 알림 문구에 코트 이름이 들어간다")
    void gameStartMessageContainsCourtName() {
        List<NotificationRequest> requests = strategy.convert(
                GameStartedEvent.started(1L, 10L, "모임", "image-key", "화이팅코트", List.of(20L)));

        assertThat(requests.get(0).content()).isEqualTo("'화이팅코트' 입장해주세요!");
    }

    @Test
    @DisplayName("게임 진행자 지정 이벤트를 지원한다")
    void supportsGameHostAssignedEvent() {
        boolean supports = strategy.supports(
                GameHostAssignedEvent.assigned(1L, 10L, "모임", "image-key", 20L));

        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("게임 진행자 지정 알림은 지정된 본인 1명에게 게임판 destination으로 생성된다")
    void convertsGameHostAssignedNotification() {
        List<NotificationRequest> requests = strategy.convert(
                GameHostAssignedEvent.assigned(1L, 10L, "모임", "image-key", 20L));

        assertThat(requests).hasSize(1);
        NotificationRequest request = requests.get(0);
        assertThat(request.recipientMemberId()).isEqualTo(20L);
        assertThat(request.source()).isEqualTo(NotificationSource.GAME);
        assertThat(request.content()).isEqualTo("게임 진행자로 지정되었습니다.");
        assertThat(request.destination().resourceType())
                .isEqualTo(NotificationResourceType.GAME_BOARD);
        assertThat(request.destination().resourceId()).isEqualTo(1L);
        assertThat(request.destination().action()).isEqualTo(NotificationAction.VIEW);
    }
}
