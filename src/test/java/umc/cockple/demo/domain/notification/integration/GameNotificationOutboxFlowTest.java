package umc.cockple.demo.domain.notification.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.game.events.GameHostAssignedEvent;
import umc.cockple.demo.domain.game.events.GameStartedEvent;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationSource;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxEventType;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationOutboxRepository;
import umc.cockple.demo.support.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게임 도메인 이벤트가 실제 Spring 컨텍스트에서 BEFORE_COMMIT 리스너 -> GameNotificationStrategy ->
 * NotificationOutbox 적재까지 이어지는지 검증하는 흐름 통합 테스트.
 * (전략 변환 자체는 GameNotificationStrategyTest 유닛 테스트에서 이미 검증한다)
 */
@DisplayName("게임판 알림 outbox 적재 흐름 (BEFORE_COMMIT)")
class GameNotificationOutboxFlowTest extends IntegrationTestBase {

    private static final Long GAME_BOARD_ID = 4100L;
    private static final Long PARTY_ID = 900L;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private NotificationOutboxRepository notificationOutboxRepository;

    @AfterEach
    void tearDown() {
        notificationOutboxRepository.deleteAll();
    }

    @Test
    @DisplayName("게임 시작 이벤트가 커밋되면 참가 회원 수만큼 GAME_STARTED outbox가 GAME_BOARD destination으로 적재된다")
    void gameStarted_recordsOutboxForEachMember() {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(GameStartedEvent.started(
                        GAME_BOARD_ID, PARTY_ID, "우리모임", "img-key", "화이팅코트",
                        List.of(11L, 22L))));

        List<NotificationOutbox> outboxes = notificationOutboxRepository.findAll();
        assertThat(outboxes).hasSize(2);
        assertThat(outboxes).allSatisfy(outbox -> {
            assertThat(outbox.getEventType()).isEqualTo(NotificationOutboxEventType.GAME_STARTED);
            assertThat(outbox.getSource()).isEqualTo(NotificationSource.GAME);
            assertThat(outbox.getResourceType()).isEqualTo(NotificationResourceType.GAME_BOARD);
            assertThat(outbox.getResourceId()).isEqualTo(GAME_BOARD_ID);
            assertThat(outbox.getAction()).isEqualTo(NotificationAction.VIEW);
            assertThat(outbox.getTitle()).isEqualTo("우리모임");
            assertThat(outbox.getContent()).isEqualTo("'화이팅코트' 입장해주세요!");
            assertThat(outbox.getLegacyPartyId()).isEqualTo(PARTY_ID);
        });
        assertThat(outboxes).extracting(NotificationOutbox::getRecipientMemberId)
                .containsExactlyInAnyOrder(11L, 22L);
    }

    @Test
    @DisplayName("게임 진행자 지정 이벤트가 커밋되면 지정된 본인에게 GAME_HOST_ASSIGNED outbox가 적재된다")
    void gameHostAssigned_recordsOutboxForNewHost() {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(GameHostAssignedEvent.assigned(
                        GAME_BOARD_ID, PARTY_ID, "우리모임", "img-key", 33L)));

        List<NotificationOutbox> outboxes = notificationOutboxRepository.findAll();
        assertThat(outboxes).hasSize(1);
        NotificationOutbox outbox = outboxes.get(0);
        assertThat(outbox.getEventType()).isEqualTo(NotificationOutboxEventType.GAME_HOST_ASSIGNED);
        assertThat(outbox.getSource()).isEqualTo(NotificationSource.GAME);
        assertThat(outbox.getRecipientMemberId()).isEqualTo(33L);
        assertThat(outbox.getResourceType()).isEqualTo(NotificationResourceType.GAME_BOARD);
        assertThat(outbox.getResourceId()).isEqualTo(GAME_BOARD_ID);
        assertThat(outbox.getAction()).isEqualTo(NotificationAction.VIEW);
        assertThat(outbox.getContent()).isEqualTo("게임 진행자로 지정되었습니다.");
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 게임 알림 outbox가 적재되지 않는다")
    void rolledBack_recordsNothing() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(GameStartedEvent.started(
                    GAME_BOARD_ID, PARTY_ID, "우리모임", "img-key", "화이팅코트", List.of(11L)));
            status.setRollbackOnly();
        });

        assertThat(notificationOutboxRepository.findAll()).isEmpty();
    }
}
