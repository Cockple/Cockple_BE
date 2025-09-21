package umc.cockple.demo.domain.notification.enums;

public enum NotificationType {
    INVITE, // 초대 버튼 있는 알림인데 안 읽은 경우
    INVITE_ACCEPT,// 초대 버튼 있는 알림이고 승인한 경우
    INVITE_REJECT, // 초대 버튼 있는 알림이고 거절한 경우

    CHANGE, // 눌렀을 떄 운동 / 모임 상세 페이지로 가는 알림
    SIMPLE // 읽기 전용 단순 알림
}
