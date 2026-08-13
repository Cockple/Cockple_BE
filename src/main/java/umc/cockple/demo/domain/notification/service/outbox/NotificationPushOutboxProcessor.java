package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushChannel;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushOutboxStatus;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushTargetType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationPushOutboxRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.service.ChatPushNotificationService;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushOutboxProcessor {

    private final NotificationPushOutboxRepository repository;
    private final NotificationPushOutboxClaimService claimService;
    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;
    private final ChatPushNotificationService chatPushNotificationService;
    private final ObjectMapper objectMapper;

    @Value("${cockple.notification-push-outbox.batch-size:50}")
    private int batchSize;

    @Value("${cockple.notification-push-outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${cockple.notification-push-outbox.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    public int processPendingBatch() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<Long> ids = repository.findClaimCandidateIds(
                NotificationPushOutboxStatus.retryableStatuses(),
                NotificationPushOutboxStatus.PROCESSING,
                maxRetryCount,
                timeoutBefore,
                PageRequest.of(0, batchSize)
        );

        int processedCount = 0;
        for (Long id : ids) {
            if (processOne(id)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    public boolean processOne(Long outboxId) {
        Optional<ClaimedNotificationPushOutbox> claimed = claimService.claim(
                outboxId,
                maxRetryCount,
                LocalDateTime.now().minusMinutes(processingTimeoutMinutes)
        );
        return claimed.map(this::process).orElse(false);
    }

    private boolean process(ClaimedNotificationPushOutbox claimed) {
        NotificationPushOutbox outbox = claimed.outbox();
        try {
            if (outbox.getChannel() != NotificationPushChannel.FCM) {
                throw new IllegalArgumentException("지원하지 않는 Push 채널입니다: " + outbox.getChannel());
            }

            if (outbox.getTargetType() == NotificationPushTargetType.CHAT) {
                sendChat(outbox);
            } else {
                Optional<Notification> notificationOptional = notificationRepository
                        .findByIdWithMember(outbox.getNotificationId());
                if (notificationOptional.isEmpty()) {
                    claimService.markDead(claimed, "알림을 찾을 수 없습니다. id=" + outbox.getNotificationId());
                    return true;
                }
                Notification notification = notificationOptional.get();
                Member member = notification.getMember();
                if (!member.isWithdrawn()) {
                    fcmService.sendNotificationWithRetry(member, notification.getTitle(), notification.getContent());
                }
            }

            return claimService.markDone(claimed);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                claimService.markDead(claimed, e.getMessage());
            } else {
                claimService.markFailed(claimed, e.getMessage());
            }
            log.warn("Notification Push outbox 처리 실패 - outboxId: {}", outbox.getId(), e);
            return true;
        }
    }

    private void sendChat(NotificationPushOutbox outbox) throws Exception {
        List<Long> activeSubscriberIds = objectMapper.readValue(
                outbox.getActiveSubscriberIds(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
        );
        chatPushNotificationService.sendPushWithRetry(ChatNotificationEvent.create(
                outbox.getChatRoomId(),
                outbox.getChatRoomType(),
                outbox.getTitle(),
                outbox.getContent(),
                outbox.getSenderId(),
                activeSubscriberIds
        ));
    }
}
