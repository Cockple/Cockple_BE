package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import umc.cockple.demo.domain.notification.converter.NotificationConverter;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationListResponseDTO;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;


    public NotificationListResponseDTO getAllNotifications(Long memberId, Long cursor, int size) {
        // 회원 조회
        Member member = findByMemberId(memberId);

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Notification> rows = notificationRepository.findPageByMember(member, cursor, pageable);

        boolean hasNext = rows.size() > size;
        List<Notification> page = hasNext ? rows.subList(0, size) : rows;

        // dto 매핑
        List<AllNotificationsResponseDTO> notifications = page.stream()
                .map(notification -> {
                    String url = fileService.getUrlFromKey(notification.getImageKey());
                    return NotificationConverter.toAllNotificationResponseDTO(notification, url);
                })
                .toList();

        Long nextCursor = hasNext && !page.isEmpty()
                ? page.get(page.size() - 1).getId() : null;

        int totalElements = (int) notificationRepository.countByMember(member);

        return NotificationConverter.toNotificationListResponse(notifications, hasNext, nextCursor, totalElements);
    }


    public ExistNewNotificationResponseDTO checkUnreadNotification(Long memberId) {
        boolean existNewNotification = notificationRepository.existsByMember_IdAndIsReadFalse(memberId);
        return ExistNewNotificationResponseDTO.builder()
                .existNewNotification(existNewNotification)
                .build();
    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }


}
