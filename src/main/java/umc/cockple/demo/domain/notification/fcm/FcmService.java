package umc.cockple.demo.domain.notification.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final MemberRepository memberRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Transactional
    public void registerFcmToken(Long memberId, String fcmToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.updateFcmToken(fcmToken);
        log.info("FCM 토큰 등록 완료 - memberId: {}", memberId);
    }

    public void sendNotification(Member member, String title, String content) {
        String fcmToken = member.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.info("FCM 토큰 없음 - memberId: {}, 알림 전송 생략", member.getId());
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(content)
                        .build())
                .build();

        try {
            firebaseMessaging.send(message);
            log.info("FCM 전송 완료 - memberId: {}", member.getId());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 - memberId: {}, error: {}", member.getId(), e.getMessage());
        }
    }
}
