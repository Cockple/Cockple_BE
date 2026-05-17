package umc.cockple.demo.domain.notification.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.notification.fcm.FcmService;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleNotification(NotificationEvent event) {
        log.info("[NOTIFICATION] FCM 전송 이벤트 처리 - memberId: {}", event.member().getId());
        try {
            fcmService.sendNotification(event.member(), event.title(), event.content());
        } catch (Exception e) {
            log.error("[NOTIFICATION] FCM 전송 실패 - memberId: {}", event.member().getId(), e);
        }
    }
}
