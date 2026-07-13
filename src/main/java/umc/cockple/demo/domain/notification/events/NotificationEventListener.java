package umc.cockple.demo.domain.notification.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final FcmService fcmService;
    private final MemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    // @Transactional 두지 않음: FCM(외부 I/O) 호출 동안 DB 커넥션을 점유하지 않기 위함.
    // findById는 리포지토리 자체 트랜잭션으로 커넥션을 짧게 잡았다 반납한다.
    public void handleNotification(NotificationEvent event) {
        log.info("[NOTIFICATION] FCM 전송 이벤트 처리 - memberId: {}", event.memberId());
        memberRepository.findById(event.memberId()).ifPresentOrElse(
                member -> sendFcm(member, event),
                () -> log.warn("[NOTIFICATION] FCM 전송 대상 멤버 없음 - memberId: {}", event.memberId())
        );
    }

    private void sendFcm(Member member, NotificationEvent event) {
        if (member.isWithdrawn()) {
            log.info("[NOTIFICATION] 탈퇴 회원 FCM 전송 생략 - memberId: {}", event.memberId());
            return;
        }
        try {
            fcmService.sendNotification(member, event.title(), event.content());
        } catch (Exception e) {
            log.error("[NOTIFICATION] FCM 전송 실패 - memberId: {}", event.memberId(), e);
        }
    }
}
