package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.command.outbox.NotificationOutboxPayload;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationOutboxRepository;
import umc.cockple.demo.domain.notification.strategy.NotificationStrategyRegistry;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationStrategyRegistry notificationStrategyRegistry;

    public void record(Object event) {
        List<NotificationOutbox> outboxes = notificationStrategyRegistry.convert(event).stream()
                .map(this::toOutbox)
                .toList();

        if (outboxes.isEmpty()) {
            return;
        }

        notificationOutboxRepository.saveAll(outboxes);
        log.info("Notification outbox 등록 완료 - eventId: {}, 등록 수: {}",
                outboxes.get(0).getEventId(), outboxes.size());
    }

    private NotificationOutbox toOutbox(NotificationRequest request) {
        String notificationKey = Objects.requireNonNull(request.eventId(), "알림 이벤트 ID가 없습니다.")
                + ":" + request.recipientMemberId();

        return NotificationOutbox.pending(new NotificationOutboxPayload(
                request.eventId(),
                Objects.requireNonNull(request.eventType(), "알림 Outbox 이벤트 타입이 없습니다."),
                request.source(),
                request.recipientMemberId(),
                request.title(),
                request.content(),
                request.imageKey(),
                request.data(),
                request.destination(),
                request.legacyCompatibility(),
                notificationKey
        ));
    }
}
