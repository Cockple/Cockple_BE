package umc.cockple.demo.domain.push.domain;

import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.push.command.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.push.enums.NotificationPushChannel;
import umc.cockple.demo.domain.push.enums.NotificationPushOutboxStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPushOutboxTest {

    @Test
    void pending_상태로_생성된다() {
        NotificationPushOutbox outbox = NotificationPushOutbox.pending(
                new NotificationPushOutboxPayload(10L, NotificationPushChannel.FCM));

        assertThat(outbox.getNotificationId()).isEqualTo(10L);
        assertThat(outbox.getStatus()).isEqualTo(NotificationPushOutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isZero();
    }

    @Test
    void 실패하면_retry_count가_증가한다() {
        NotificationPushOutbox outbox = NotificationPushOutbox.pending(
                new NotificationPushOutboxPayload(10L, NotificationPushChannel.FCM));

        outbox.markProcessing(LocalDateTime.now(), "token");
        outbox.markFailed("FCM 장애");

        assertThat(outbox.getStatus()).isEqualTo(NotificationPushOutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("FCM 장애");
    }

    @Test
    void 완료하면_DONE이_되고_에러가_초기화된다() {
        NotificationPushOutbox outbox = NotificationPushOutbox.pending(
                new NotificationPushOutboxPayload(10L, NotificationPushChannel.FCM));
        outbox.markProcessing(LocalDateTime.now(), "token");
        outbox.markFailed("FCM 장애");

        outbox.markDone();

        assertThat(outbox.getStatus()).isEqualTo(NotificationPushOutboxStatus.DONE);
        assertThat(outbox.getLastError()).isNull();
        assertThat(outbox.getLastAttemptedAt()).isNotNull();
    }
}
