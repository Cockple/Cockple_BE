package umc.cockple.demo.domain.push.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import umc.cockple.demo.domain.push.service.NotificationPushOutboxService;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {

    private final NotificationPushOutboxService notificationPushOutboxService;

    // 채팅 메시지 저장과 Push Outbox 적재를 하나의 트랜잭션으로 묶기 위해
    // BEFORE_COMMIT 단계에서 동기적으로 처리한다. 적재가 실패하면 메시지 저장까지 함께 롤백된다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        notificationPushOutboxService.enqueueChat(event);
    }
}
