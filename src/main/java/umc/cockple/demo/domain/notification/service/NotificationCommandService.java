package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.MarkAsReadDTO;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.global.enums.NotificationType;

import static umc.cockple.demo.domain.notification.dto.MarkAsReadDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;



    public Response markAsReadNotification(Long memberId, Long notificationId, NotificationType type) {
        Notification notification = findByNotificationId(notificationId);

        if (!notification.getMember().getId().equals(memberId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_OWNED);
        }

        notification.changeType(type);

        notification.read();

        return new Response(notification.getType());
    }


    private Notification findByNotificationId(Long notification) {
        return notificationRepository.findById(notification)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

}
