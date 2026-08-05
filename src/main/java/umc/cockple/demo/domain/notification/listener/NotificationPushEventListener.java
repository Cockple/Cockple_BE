package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.event.NotificationPushRequestedEvent;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPushEventListener {

    private final FcmService fcmService;
    private final NotificationRepository notificationRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationPushExecutor")
    public void handle(NotificationPushRequestedEvent event) {
        log.info("[NOTIFICATION V2] FCM 전송 이벤트 처리 - notificationId: {}", event.notificationId());
        notificationRepository.findByIdWithMember(event.notificationId()).ifPresentOrElse(
                this::sendFcm,
                () -> log.warn("[NOTIFICATION V2] FCM 전송 알림 없음 - notificationId: {}", event.notificationId())
        );
    }

    private void sendFcm(Notification notification) {
        var member = notification.getMember();
        if (member.isWithdrawn()) {
            log.info("[NOTIFICATION V2] 탈퇴 회원 FCM 전송 생략 - memberId: {}", member.getId());
            return;
        }
        try {
            fcmService.sendNotification(member, notification.getTitle(), notification.getContent());
        } catch (Exception e) {
            log.error("[NOTIFICATION V2] FCM 전송 실패 - notificationId: {}", notification.getId(), e);
        }
    }
}
