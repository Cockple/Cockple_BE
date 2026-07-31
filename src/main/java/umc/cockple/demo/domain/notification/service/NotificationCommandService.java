package umc.cockple.demo.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.listener.event.NotificationEvent;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static umc.cockple.demo.domain.notification.dto.MarkAsReadDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    // 보관 정책: 회원당 알림을 대략 RETENTION_CAP개로 유지
    // 매 생성마다 정리하면 정상 상태에서도 삭제가 발생해 쓰기 경합이 생기므로, RETENTION_TRIGGER를 넘을 때만 CAP까지 한 번에 정리한다(슬랙/히스테리시스)
    private static final int RETENTION_CAP = 50;
    private static final int RETENTION_TRIGGER = 60;
    private static final int INVITE_RETENTION_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final PartyRepository partyRepository;
    private final NotificationMessageGenerator notificationMessageGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final LegacyNotificationDestinationMapper legacyNotificationDestinationMapper;

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

    public void createNotification(CreateNotificationRequestDTO dto) {
        try {
            long start = System.currentTimeMillis();
            Member member = dto.member();
            enforceRetentionPolicy(member);

            Party party = partyRepository.findById(dto.partyId())
                    .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

            Map<String, Object> context = new HashMap<>();
            if (dto.exerciseId() != null) context.put("exerciseId", dto.exerciseId());
            if (dto.exerciseDate() != null) context.put("exerciseDate", dto.exerciseDate());
            if (dto.invitationId() != null) context.put("invitationId", dto.invitationId());

            String content;
            String title = party.getPartyName();
            if (dto.target() == NotificationTarget.EXERCISE_DELETE) {
                String result = extractExerciseDateFormat(dto.exerciseDate());
                content = notificationMessageGenerator.generateExerciseDeletedMessage(result);
            } else if (dto.target() == NotificationTarget.EXERCISE_MODIFY) {
                String result = extractExerciseDateFormat(dto.exerciseDate());
                content = notificationMessageGenerator.generateExerciseChangedMessage(result);
            } else if (dto.target() == NotificationTarget.EXERCISE_ATTENDANCE) {
                content = notificationMessageGenerator.generateExerciseAttendChangedMessage();
            } else if (dto.target() == NotificationTarget.PARTY_DELETE) {
                content = notificationMessageGenerator.generatePartyDeletedMessage();
            } else if (dto.target() == NotificationTarget.PARTY_MODIFY) {
                content = notificationMessageGenerator.generatePartyInfoChangedMessage();
            } else if (dto.target() == NotificationTarget.PARTY_INVITE) {
                content = notificationMessageGenerator.generateInviteMessage(party.getPartyName());
                title = "새로운 모임";
            } else if (dto.target() == NotificationTarget.PARTY_INVITE_APPROVED) {
                content = notificationMessageGenerator.generateInviteApprovedMessage(dto.subjectName());
            } else if (dto.target() == NotificationTarget.PARTY_SUBOWNER_ASSIGNED) {
                content = notificationMessageGenerator.generateSubOwnerAssignedMessage(dto.subjectName());
            } else if (dto.target() == NotificationTarget.PARTY_SUBOWNER_RELEASED) {
                content = notificationMessageGenerator.generateSubOwnerReleasedMessage(dto.subjectName());
            } else {
                content = notificationMessageGenerator.generateJoinRequestApprovedMessage();
            }

            String data = objectMapper.writeValueAsString(context);
            NotificationDestination destination = legacyNotificationDestinationMapper.map(dto);

            Notification notification = Notification.builder()
                    .member(member)
                    .partyId(dto.partyId())
                    .resourceType(destination != null ? destination.resourceType() : null)
                    .resourceId(destination != null ? destination.resourceId() : null)
                    .action(destination != null ? destination.action() : null)
                    .title(title)
                    .content(content)
                    .type(dto.target().getDefaultType())
                    .isRead(false)
                    .imageKey(party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null)
                    .data(data)
                    .build();

            notificationRepository.save(notification);
            log.info("[NOTIFICATION] DB 저장 완료 - memberId: {}, 소요시간: {}ms",
                    member.getId(), System.currentTimeMillis() - start);

            eventPublisher.publishEvent(new NotificationEvent(member.getId(), title, content));
            log.info("[NOTIFICATION] 알림 이벤트 발행 완료 - memberId: {}, 총 소요시간: {}ms",
                    member.getId(), System.currentTimeMillis() - start);

        } catch (JsonProcessingException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DATA);
        }
    }

    private void enforceRetentionPolicy(Member member) {
        long count = notificationRepository.countByMember(member);
        if (count < RETENTION_TRIGGER) {
            return;
        }

        int excess = (int) (count - RETENTION_CAP);
        List<Long> deletableIds = notificationRepository.findDeletableIdsOldestFirst(
                member,
                NotificationType.INVITE,
                LocalDateTime.now().minusDays(INVITE_RETENTION_DAYS),
                PageRequest.of(0, excess));

        if (!deletableIds.isEmpty()) {
            notificationRepository.deleteAllByIdInBatch(deletableIds);
        }
    }

    private Notification findByNotificationId(Long notification) {
        return notificationRepository.findById(notification)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private String extractExerciseDateFormat(LocalDate date) {
        // 날짜 요일 포매팅 (MM.dd(요일))
        String format = date.format(DateTimeFormatter.ofPattern("MM.dd"));
        String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        return format + "(" + day + ")";
    }

}
