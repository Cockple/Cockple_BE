package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.notification.command.outbox.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushChannel;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationPushOutboxRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationPushOutboxService {

    private final NotificationPushOutboxRepository notificationPushOutboxRepository;

    public void enqueue(Long notificationId) {
        notificationPushOutboxRepository.save(
                NotificationPushOutbox.pending(
                        new NotificationPushOutboxPayload(notificationId, NotificationPushChannel.FCM)
                )
        );
    }
}
