package umc.cockple.demo.domain.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByNotificationKey(String notificationKey);

    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.member
            WHERE n.id = :notificationId
            """)
    Optional<Notification> findByIdWithMember(@Param("notificationId") Long notificationId);

    boolean existsByMember_IdAndIsReadFalse(Long memberId);

    long countByMember(Member member);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.member = :member
              AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
            """)
    List<Notification> findPageByMember(@Param("member") Member member,
                                        @Param("cursor") Long cursor,
                                        Pageable pageable);

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

    @Query("""
            SELECT n.id FROM Notification n
            WHERE n.member = :member
              AND (n.resourceType IS NULL
                   OR n.resourceType <> :invitation
                   OR n.createdAt < :threshold)
            ORDER BY n.createdAt ASC
            """)
    List<Long> findDeletableIdsOldestFirstByResourceType(
            @Param("member") Member member,
            @Param("invitation") NotificationResourceType invitation,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable);
}
