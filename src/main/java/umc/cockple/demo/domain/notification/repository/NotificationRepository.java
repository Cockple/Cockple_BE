package umc.cockple.demo.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
