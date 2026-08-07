package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import umc.cockple.demo.domain.notification.service.ChatPushNotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationEventListener {

    private final ChatPushNotificationService chatPushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationPushExecutor")
    public void handleChatNotification(ChatNotificationEvent event) {
        log.info("채팅 알림 이벤트 처리 - 채팅방: {}, 발신자: {}", event.chatRoomId(), event.senderId());
        try {
            chatPushNotificationService.sendPush(event);
        } catch (Exception e) {
            log.error("채팅 알림 이벤트 처리 중 오류 발생 - 채팅방: {}", event.chatRoomId(), e);
        }
    }
}
