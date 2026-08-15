package umc.cockple.demo.domain.notification.service.outbox;

import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;

public interface NotificationOutboxHandler {

    boolean supports(NotificationOutbox outbox);

    void handle(NotificationOutbox outbox);
}
