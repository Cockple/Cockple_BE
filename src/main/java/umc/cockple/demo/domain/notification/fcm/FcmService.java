package umc.cockple.demo.domain.notification.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private static final int FCM_MULTICAST_MAX = 500;   // MulticastMessage 토큰 한도

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
     * 채팅 알림을 수신자 전체에게 한 번의 요청(fan-out)
     */
    public void sendChatMulticast(List<Member> recipients, String title, String content,
                                  Long chatRoomId, ChatRoomType chatRoomType) {
        List<String> tokens = recipients.stream()
                .map(Member::getFcmToken)
                .filter(token -> token != null && !token.isBlank())
                .toList();

        if (tokens.isEmpty()) {
            log.info("[FCM] 채팅 멀티캐스트 대상 토큰 없음 - chatRoomId: {}", chatRoomId);
            return;
        }

        // 부하테스트용 지연 주입
        if (fakeLatencyMs > 0) {
            sleepQuietly(fakeLatencyMs);
            log.info("[FCM][FAKE] 멀티캐스트 지연주입 {}ms 후 실전송 스킵 - chatRoomId: {}, 수신자: {}",
                    fakeLatencyMs, chatRoomId, tokens.size());
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(content)
                .build();
        WebpushConfig webpushConfig = WebpushConfig.builder()
                .setFcmOptions(WebpushFcmOptions.withLink(buildChatLink(chatRoomType, chatRoomId)))
                .build();

        long start = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;

        for (int from = 0; from < tokens.size(); from += FCM_MULTICAST_MAX) {
            List<String> chunk = tokens.subList(from, Math.min(from + FCM_MULTICAST_MAX, tokens.size()));
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(notification)
                    .putData("chatRoomId", chatRoomId.toString())
                    .putData("chatRoomType", chatRoomType.name())
                    .setWebpushConfig(webpushConfig)
                    .build();
            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();
            } catch (FirebaseMessagingException e) {
                failureCount += chunk.size();
                log.error("[FCM] 채팅 멀티캐스트 청크 전송 실패 - chatRoomId: {}, size: {}, error: {}",
                        chatRoomId, chunk.size(), e.getMessage());
            }
        }

        log.info("[FCM] 채팅 멀티캐스트 전송 완료 - chatRoomId: {}, 수신자: {}, 성공: {}, 실패: {}, 소요시간: {}ms",
                chatRoomId, tokens.size(), successCount, failureCount, System.currentTimeMillis() - start);
    }

    // 부하테스트용 FCM 지연 주입
    private boolean applyFakeLatencyAndSkip(Long memberId) {
        if (fakeLatencyMs <= 0) {
            return false;
        }
        sleepQuietly(fakeLatencyMs);
        log.info("[FCM][FAKE] 지연주입 {}ms 후 실전송 스킵 - memberId: {}", fakeLatencyMs, memberId);
        return true;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildChatLink(ChatRoomType chatRoomType, Long chatRoomId) {
        return switch (chatRoomType) {
            case PARTY -> "/chat/group/" + chatRoomId;
            case DIRECT -> "/chat/personal/" + chatRoomId;
        };
    }
}
