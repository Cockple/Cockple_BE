package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.enums.NotificationType;

import static umc.cockple.demo.domain.notification.dto.MarkAsReadDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;


    // 알림 타입 변경 (초대 수락, 거절에 사용)
    public Response markAsReadNotification(Long memberId, Long notificationId, NotificationType type) {
        Notification notification = findByNotificationId(notificationId);

        if (!notification.getMember().getId().equals(memberId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_OWNED);
        }

        notification.changeType(type);

        notification.read();

        return new Response(notification.getType());
    }

    // 알림 추가 로직
    /*
    * 모임에 초대 받았을때
       모임에 가입 허가 받았을때

    (내모임)
    모임 정보가 수정되었을때
    모임이 삭제되었을때

    (신청한 운동에서 변경사항)
    내가 참여하거나 내 게스트가 참여하는 운동 정보가 수정되었을때
    내가 참여하거나 내 게스트가 참여하는 운동 정보가 수정되었을때
    내가 참여하거나 내 게스트가 참여하는 운동에서 대기 > 참석으로 변경되었을때 ( 알림 멘트 동일
    * */
    public void createNotification() {

    }


    private Notification findByNotificationId(Long notification) {
        return notificationRepository.findById(notification)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

}
