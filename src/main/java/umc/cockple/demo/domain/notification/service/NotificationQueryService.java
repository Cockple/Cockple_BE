package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.controller.NotificationController;
import umc.cockple.demo.domain.notification.converter.NotificationConverter;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;


    public List<AllNotificationsResponseDTO> getAllNotifications(Long memberId) {
        // 회원 조회
        Member member = findByMemberId(memberId);

        // 회원의 모든 알림 조회
        List<Notification> notifications = notificationRepository.findAllByMember(member);

        // dto 매핑 및 반환
        return notifications.stream()
                .map(NotificationConverter::toAllNotificationResponseDTO)
                .toList();
    }

    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
