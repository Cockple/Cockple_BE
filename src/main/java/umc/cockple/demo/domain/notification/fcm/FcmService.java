package umc.cockple.demo.domain.notification.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
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

    @Value("${fcm.fake-latency-ms:0}")
    private long fakeLatencyMs;

    @Transactional
    public void registerFcmToken(Long memberId, String fcmToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.updateFcmToken(fcmToken);
        log.info("FCM 토큰 등록 완료 - memberId: {}", memberId);
    }

    public void sendNotification(Member member, String title, String content) {
        if (applyFakeLatencyAndSkip(member.getId())) {
            return;
        }

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
            long start = System.currentTimeMillis();
            firebaseMessaging.send(message);
            log.info("[FCM] 일반 알림 전송 완료 - memberId: {}, 소요시간: {}ms", member.getId(), System.currentTimeMillis() - start);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 - memberId: {}, error: {}", member.getId(), e.getMessage());
        }
    }

    public void sendChatNotification(Member member, String title, String content,
                                     Long chatRoomId, ChatRoomType chatRoomType) {
        if (applyFakeLatencyAndSkip(member.getId())) {
            return;
        }

        String fcmToken = member.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.info("FCM 토큰 없음 - memberId: {}, 채팅 알림 전송 생략", member.getId());
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(content)
                        .build())
                .putData("chatRoomId", chatRoomId.toString())
                .putData("chatRoomType", chatRoomType.name())
                .setWebpushConfig(WebpushConfig.builder()
                        .setFcmOptions(WebpushFcmOptions.withLink(buildChatLink(chatRoomType, chatRoomId)))
                        .build())
                .build();

        try {
            long start = System.currentTimeMillis();
            firebaseMessaging.send(message);
            log.info("[FCM] 채팅 알림 전송 완료 - memberId: {}, chatRoomId: {}, 소요시간: {}ms", member.getId(), chatRoomId, System.currentTimeMillis() - start);
        } catch (FirebaseMessagingException e) {
            log.error("채팅 FCM 전송 실패 - memberId: {}, error: {}", member.getId(), e.getMessage());
        }
    }

    /**
     * 부하테스트용 FCM 지연 주입.
     */
    private boolean applyFakeLatencyAndSkip(Long memberId) {
        if (fakeLatencyMs <= 0) {
            return false;
        }
        try {
            Thread.sleep(fakeLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[FCM][FAKE] 지연주입 {}ms 후 실전송 스킵 - memberId: {}", fakeLatencyMs, memberId);
        return true;
    }

    /**
     * 채팅 알림 클릭 시 이동할 경로(상대경로)를 생성한다.
     * webpush.fcm_options.link 로 전달되어 FCM이 클릭 시 네이티브로 이동시킨다.
     */
    private String buildChatLink(ChatRoomType chatRoomType, Long chatRoomId) {
        return switch (chatRoomType) {
            case PARTY -> "/chat/group/" + chatRoomId;
            case DIRECT -> "/chat/personal/" + chatRoomId;
        };
    }
}
