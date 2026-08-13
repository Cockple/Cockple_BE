package umc.cockple.demo.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.notification.domain.NotificationOutbox;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
}
