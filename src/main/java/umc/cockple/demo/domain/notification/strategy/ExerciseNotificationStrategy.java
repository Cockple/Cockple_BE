package umc.cockple.demo.domain.notification.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseDeletedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseUpdatedEvent;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationSource;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.service.NotificationMessageGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExerciseNotificationStrategy implements NotificationEventStrategy {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");

    private final NotificationMessageGenerator notificationMessageGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Object event) {
        return event instanceof ExerciseDeletedEvent
                || event instanceof ExerciseUpdatedEvent
                || event instanceof ExerciseAttendanceChangedEvent;
    }

    @Override
    public List<NotificationRequest> convert(Object event) {
        if (event instanceof ExerciseDeletedEvent deletedEvent) {
            String date = formatDate(deletedEvent.exerciseDate());
            return deletedEvent.recipientMemberIds().stream()
                    .map(memberId -> request(
                            memberId,
                            deletedEvent.partyId(),
                            deletedEvent.partyName(),
                            deletedEvent.imageKey(),
                            deletedEvent.exerciseId(),
                            deletedEvent.exerciseDate(),
                            notificationMessageGenerator.generateExerciseDeletedMessage(date),
                            null,
                            NotificationType.SIMPLE
                    ))
                    .toList();
        }

        if (event instanceof ExerciseUpdatedEvent updatedEvent) {
            String date = formatDate(updatedEvent.exerciseDate());
            return updatedEvent.recipientMemberIds().stream()
                    .map(memberId -> request(
                            memberId,
                            updatedEvent.partyId(),
                            updatedEvent.partyName(),
                            updatedEvent.imageKey(),
                            updatedEvent.exerciseId(),
                            updatedEvent.exerciseDate(),
                            notificationMessageGenerator.generateExerciseChangedMessage(date),
                            destination(updatedEvent.exerciseId()),
                            NotificationType.CHANGE
                    ))
                    .toList();
        }

        if (event instanceof ExerciseAttendanceChangedEvent attendanceEvent) {
            return attendanceEvent.recipientMemberIds().stream()
                    .map(memberId -> request(
                            memberId,
                            attendanceEvent.partyId(),
                            attendanceEvent.partyName(),
                            attendanceEvent.imageKey(),
                            attendanceEvent.exerciseId(),
                            attendanceEvent.exerciseDate(),
                            notificationMessageGenerator.generateExerciseAttendChangedMessage(),
                            destination(attendanceEvent.exerciseId()),
                            NotificationType.CHANGE
                    ))
                    .toList();
        }

        throw new IllegalArgumentException("지원하지 않는 운동 이벤트입니다: " + event.getClass().getName());
    }

    private NotificationRequest request(
            Long recipientMemberId,
            Long partyId,
            String partyName,
            String imageKey,
            Long exerciseId,
            LocalDate exerciseDate,
            String content,
            NotificationDestination destination,
            NotificationType legacyType
    ) {
        return new NotificationRequest(
                NotificationSource.EXERCISE,
                recipientMemberId,
                partyName,
                content,
                imageKey,
                serializeData(exerciseId, exerciseDate),
                destination,
                new NotificationLegacyCompatibility(partyId, legacyType)
        );
    }

    private String serializeData(Long exerciseId, LocalDate exerciseDate) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "exerciseId", exerciseId,
                    "exerciseDate", exerciseDate
            ));
        } catch (JsonProcessingException e) {
            log.error("운동 알림 데이터 생성 실패 - exerciseId: {}", exerciseId, e);
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DATA);
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER)
                + "(" + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
    }

    private NotificationDestination destination(Long exerciseId) {
        return new NotificationDestination(
                NotificationResourceType.EXERCISE,
                exerciseId,
                NotificationAction.VIEW
        );
    }
}
