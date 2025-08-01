package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.controller.NotificationController;
import umc.cockple.demo.domain.notification.converter.NotificationConverter;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;


    public List<AllNotificationsResponseDTO> getAllNotifications(Long memberId) {
        // 회원 조회
        Member member = findByMemberId(memberId);

        // 회원의 모든 알림 조회
        List<Notification> notifications = notificationRepository.findAllByMember(member);

        if (notifications.isEmpty()) {
            return List.of();
        }
        // dto 매핑 및 반환
        return notifications.stream()
                .map(notification -> {
                    String url = imageService.getUrlFromKey(notification.getImageKey());
                    return NotificationConverter.toAllNotificationResponseDTO(notification, url);
                })
                .toList();
    }


    public ExistNewNotificationResponseDTO checkUnreadNotification(Long memberId) {
        Member member = findByMemberId(memberId);

        List<Notification> notifications = member.getNotifications();

        long count = notifications.stream()
                .filter(notification -> notification.getIsRead().equals(false))
                .count();

        if (count > 0) {
            return ExistNewNotificationResponseDTO.builder()
                    .existNewNotification(true)
                    .build();
        } else {
            return ExistNewNotificationResponseDTO.builder()
                    .existNewNotification(false)
                    .build();
        }
    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }


}
