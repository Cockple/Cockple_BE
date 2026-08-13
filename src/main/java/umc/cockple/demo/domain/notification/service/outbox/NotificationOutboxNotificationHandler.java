package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.notification.command.NotificationCreateCommand;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxEventType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.service.NotificationV2CommandService;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationOutboxNotificationHandler implements NotificationOutboxHandler {

    private static final Set<NotificationOutboxEventType> SUPPORTED_EVENT_TYPES =
            EnumSet.allOf(NotificationOutboxEventType.class);

    private final NotificationV2CommandService notificationV2CommandService;
    private final NotificationRepository notificationRepository;

    @Override
    public boolean supports(NotificationOutbox outbox) {
        return SUPPORTED_EVENT_TYPES.contains(outbox.getEventType());
    }

    @Override
    public void handle(NotificationOutbox outbox) {
        if (notificationRepository.existsByNotificationKey(outbox.getNotificationKey())) {
            return;
        }

        notificationV2CommandService.createNotification(
                new NotificationCreateCommand(
                        outbox.getRecipientMemberId(),
                        outbox.getTitle(),
                        outbox.getContent(),
                        outbox.getImageKey(),
                        outbox.getData(),
                        destination(outbox),
                        outbox.getNotificationKey()
                ),
                legacyCompatibility(outbox)
        );
    }

    private NotificationDestination destination(NotificationOutbox outbox) {
        if (outbox.getResourceType() == null
                && outbox.getResourceId() == null
                && outbox.getAction() == null) {
            return null;
        }
        return new NotificationDestination(
                outbox.getResourceType(), outbox.getResourceId(), outbox.getAction()
        );
    }

    private NotificationLegacyCompatibility legacyCompatibility(NotificationOutbox outbox) {
        if (outbox.getLegacyPartyId() == null && outbox.getLegacyType() == null) {
            return null;
        }
        return new NotificationLegacyCompatibility(
                outbox.getLegacyPartyId(), outbox.getLegacyType()
        );
    }
}
