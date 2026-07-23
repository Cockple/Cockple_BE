package umc.cockple.demo.domain.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByMemberOrderByCreatedAtDesc(Member member);

    boolean existsByMember_IdAndIsReadFalse(Long memberId);

    long countByMember(Member member);

    @Query("""
            SELECT n.id FROM Notification n
            WHERE n.member = :member
              AND (n.type <> :invite OR n.createdAt < :threshold)
            ORDER BY n.createdAt ASC
            """)
    List<Long> findDeletableIdsOldestFirst(@Param("member") Member member,
                                           @Param("invite") NotificationType invite,
                                           @Param("threshold") LocalDateTime threshold,
                                           Pageable pageable);
}
