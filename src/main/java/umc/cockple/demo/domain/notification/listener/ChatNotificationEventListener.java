package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import umc.cockple.demo.domain.notification.service.outbox.NotificationPushOutboxService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationEventListener {

    private final NotificationPushOutboxService notificationPushOutboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationPushExecutor")
    public void handleChatNotification(ChatNotificationEvent event) {
        try {
            notificationPushOutboxService.enqueueChat(event);
        } catch (Exception e) {
            // 채팅 메시지 저장은 성공했으므로 부가 기능인 푸시 실패가 메시지를 롤백시키지 않도록 한다.
            log.error("채팅 Push Outbox 적재 실패 - messageId: {}", event.messageId(), e);
        }
    }
}
