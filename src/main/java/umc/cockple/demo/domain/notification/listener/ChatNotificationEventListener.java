package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import umc.cockple.demo.domain.notification.service.outbox.NotificationPushOutboxService;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {

    private final NotificationPushOutboxService notificationPushOutboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        notificationPushOutboxService.enqueueChat(event);
    }
}
