package umc.cockple.demo.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.enums.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByMemberOrderByCreatedAtDesc(Member member);

    boolean existsByMember_IdAndIsReadFalse(Long memberId);

    Optional<Notification> findFirstByMemberAndTypeNotOrderByCreatedAtAsc(Member member, NotificationType type);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :id")
    void deleteByIdQuery(@Param("id") Long id);
}
