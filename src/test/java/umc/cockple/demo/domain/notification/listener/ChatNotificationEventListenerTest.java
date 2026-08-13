package umc.cockple.demo.domain.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import umc.cockple.demo.domain.notification.service.outbox.NotificationPushOutboxService;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.only;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatNotificationEventListener")
class ChatNotificationEventListenerTest {

    @Mock private NotificationPushOutboxService notificationPushOutboxService;

    @InjectMocks private ChatNotificationEventListener listener;

    private ChatNotificationEvent event() {
        return ChatNotificationEvent.create(
                10L, ChatRoomType.PARTY, "제목", "내용", 20L, List.of(30L, 40L));
    }

    @Test
    @DisplayName("채팅 알림은 Push Outbox 저장으로 위임한다")
    void handleChatNotification_delegatesToPushServiceOnly() {
        // given
        ChatNotificationEvent event = event();

        // when
        listener.handleChatNotification(event);

        then(notificationPushOutboxService).should(only()).enqueueChat(event);
    }

    @Test
    @DisplayName("Push Outbox 저장 예외는 채팅 메시지로 전파하지 않는다")
    void handleChatNotification_swallowsException() {
        // given
        ChatNotificationEvent event = event();
        willThrow(new RuntimeException("아웃박스 장애"))
                .given(notificationPushOutboxService).enqueueChat(event);

        assertThatCode(() -> listener.handleChatNotification(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("핸들러는 AFTER_COMMIT 트랜잭션 이벤트와 Push executor로 동작한다")
    void handler_usesPushExecutorAndAfterCommit() throws NoSuchMethodException {
        Method handler = ChatNotificationEventListener.class
                .getMethod("handleChatNotification", ChatNotificationEvent.class);

        Async async = handler.getAnnotation(Async.class);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("notificationPushExecutor");

        TransactionalEventListener transactional = handler.getAnnotation(TransactionalEventListener.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
