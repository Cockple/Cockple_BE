package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.converter.NotificationV2Converter;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.NotificationV2ListResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationV2ResponseDTO;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationV2QueryService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;

    public NotificationV2ListResponseDTO getAllNotifications(Long memberId, Long cursor, int size) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Notification> rows = notificationRepository.findPageByMember(member, cursor, pageable);

        boolean hasNext = rows.size() > size;
        List<Notification> page = hasNext ? rows.subList(0, size) : rows;

        List<NotificationV2ResponseDTO> notifications = page.stream()
                .map(notification -> NotificationV2Converter.toResponse(
                        notification, fileService.getUrlFromKey(notification.getImageKey())))
                .toList();

        Long nextCursor = hasNext && !page.isEmpty()
                ? page.get(page.size() - 1).getId() : null;

        int totalElements = (int) notificationRepository.countByMember(member);
        return NotificationV2Converter.toListResponse(notifications, hasNext, nextCursor, totalElements);
    }
}
