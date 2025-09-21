package umc.cockple.demo.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.enums.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByMember(Member member);

    Optional<Notification> findFirstByMemberAndTypeNotOrderByCreatedAtAsc(Member member, NotificationType type);

}
