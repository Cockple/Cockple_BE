package umc.cockple.demo.domain.push.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.push.command.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.push.domain.NotificationPushOutbox;
import umc.cockple.demo.domain.push.enums.NotificationPushChannel;
import umc.cockple.demo.domain.push.enums.NotificationPushTargetType;
import umc.cockple.demo.domain.push.repository.NotificationPushOutboxRepository;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationPushOutboxService {

    private final NotificationPushOutboxRepository notificationPushOutboxRepository;
    private final ObjectMapper objectMapper;

    public void enqueue(Long notificationId) {
        String deduplicationKey = "notification:" + notificationId + ":" + NotificationPushChannel.FCM;
        if (notificationPushOutboxRepository.existsByDeduplicationKey(deduplicationKey)) {
            return;
        }
        notificationPushOutboxRepository.save(
                NotificationPushOutbox.pending(
                        new NotificationPushOutboxPayload(notificationId, NotificationPushChannel.FCM)
                )
        );
    }

    public void enqueueChat(ChatNotificationEvent event) {
        try {
            String deduplicationKey = "chat:" + event.messageId();
            if (notificationPushOutboxRepository.existsByDeduplicationKey(deduplicationKey)) {
                return;
            }
            notificationPushOutboxRepository.save(NotificationPushOutbox.pending(
                    new NotificationPushOutboxPayload(
                            null,
                            NotificationPushChannel.FCM,
                            NotificationPushTargetType.CHAT,
                            event.chatRoomId(),
                            event.chatRoomType(),
                            event.notificationTitle(),
                            event.notificationContent(),
                            event.senderId(),
                            serialize(event.activeSubscriberIds()),
                            "chat:" + event.messageId()
                    )
            ));
        } catch (Exception e) {
            throw new IllegalStateException("채팅 Push Outbox 저장에 실패했습니다.", e);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("채팅 Push Outbox payload 생성에 실패했습니다.", e);
        }
    }
}
