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
import umc.cockple.demo.domain.notification.service.ChatPushNotificationService;

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

    @Mock private ChatPushNotificationService chatPushNotificationService;

    @InjectMocks private ChatNotificationEventListener listener;

    private ChatNotificationEvent event() {
        return ChatNotificationEvent.create(
                10L, ChatRoomType.PARTY, "제목", "내용", 20L, List.of(30L, 40L));
    }

    @Test
    @DisplayName("채팅 알림은 DB 저장 없이 ChatPushNotificationService.sendPush로만 위임한다(push-only)")
    void handleChatNotification_delegatesToPushServiceOnly() {
        // given
        ChatNotificationEvent event = event();

        // when
        listener.handleChatNotification(event);

        // then: sendPush 단 한 번만 호출되고 그 외 상호작용은 없다
        then(chatPushNotificationService).should(only()).sendPush(event);
    }

    @Test
    @DisplayName("sendPush 중 예외가 발생해도 리스너 밖으로 전파되지 않는다")
    void handleChatNotification_swallowsException() {
        // given
        ChatNotificationEvent event = event();
        willThrow(new RuntimeException("푸시 장애"))
                .given(chatPushNotificationService).sendPush(event);

        // when & then
        assertThatCode(() -> listener.handleChatNotification(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("핸들러는 AFTER_COMMIT 트랜잭션 이벤트로, notificationPushExecutor 스레드풀에서 비동기 실행된다")
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
