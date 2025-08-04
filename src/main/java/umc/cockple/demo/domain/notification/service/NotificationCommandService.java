package umc.cockple.demo.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static umc.cockple.demo.domain.notification.dto.MarkAsReadDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final NotificationMessageGenerator notificationMessageGenerator;
    private final ObjectMapper objectMapper;


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
            Party party = partyRepository.findById(dto.partyId())
                    .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

            Map<String, Object> context = new HashMap<>();
            context.put("exerciseId", dto.exerciseId());
            context.put("exerciseDate", dto.exerciseDate());

            String content;
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
            } else {
                content = notificationMessageGenerator.generateInviteAcceptedMessage();
            }

            String data = objectMapper.writeValueAsString(context);

            Notification notification = Notification.builder()
                    .member(dto.member())
                    .partyId(dto.partyId())
                    .content(content)
                    .type(dto.target().getDefaultType())
                    .isRead(false)
                    .imageKey(party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null)
                    .data(data)
                    .build();

            notificationRepository.save(notification);

        } catch (JsonProcessingException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DATA);
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
