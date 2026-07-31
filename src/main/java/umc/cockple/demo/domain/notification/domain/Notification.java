package umc.cockple.demo.domain.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Column(nullable = false)
    private Long partyId;

    @Enumerated(EnumType.STRING)
    private NotificationResourceType resourceType;

    private Long resourceId;

    @Enumerated(EnumType.STRING)
    private NotificationAction action;

    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private Boolean isRead;

    private String imageKey;

    @Column(columnDefinition = "TEXT")  // 경우마다 필요한 데이터를 저장 (JSON)
    private String data;

    public void read() {
        this.isRead = true;
    }

    public void changeType(NotificationType type) {
        this.type = type;
    }
}
