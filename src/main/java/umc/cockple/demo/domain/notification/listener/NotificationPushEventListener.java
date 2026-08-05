package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.event.NotificationPushRequestedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPushEventListener {

    private final FcmService fcmService;
    private final MemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handle(NotificationPushRequestedEvent event) {
        log.info("[NOTIFICATION V2] FCM 전송 이벤트 처리 - memberId: {}", event.memberId());
        memberRepository.findById(event.memberId()).ifPresentOrElse(
                member -> sendFcm(member, event),
                () -> log.warn("[NOTIFICATION V2] FCM 전송 대상 멤버 없음 - memberId: {}", event.memberId())
        );
    }

    private void sendFcm(Member member, NotificationPushRequestedEvent event) {
        if (member.isWithdrawn()) {
            log.info("[NOTIFICATION V2] 탈퇴 회원 FCM 전송 생략 - memberId: {}", event.memberId());
            return;
        }
        try {
            fcmService.sendNotification(member, event.title(), event.content());
        } catch (Exception e) {
            log.error("[NOTIFICATION V2] FCM 전송 실패 - memberId: {}", event.memberId(), e);
        }
    }
}
