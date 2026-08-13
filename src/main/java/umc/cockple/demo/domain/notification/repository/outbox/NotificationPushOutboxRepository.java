package umc.cockple.demo.domain.notification.repository.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;

public interface NotificationPushOutboxRepository extends JpaRepository<NotificationPushOutbox, Long> {
}
