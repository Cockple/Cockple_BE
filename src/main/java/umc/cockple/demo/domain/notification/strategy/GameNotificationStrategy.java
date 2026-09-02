package umc.cockple.demo.domain.notification.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.events.GameStartedEvent;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationSource;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxEventType;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.service.NotificationMessageGenerator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameNotificationStrategy implements NotificationEventStrategy {

    private final NotificationMessageGenerator notificationMessageGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Object event) {
        return event instanceof GameStartedEvent;
    }

    @Override
    public List<NotificationRequest> convert(Object event) {
        if (event instanceof GameStartedEvent startedEvent) {
            String content = notificationMessageGenerator.generateGameStartMessage(startedEvent.courtName());
            return startedEvent.recipientMemberIds().stream()
                    .map(memberId -> request(
                            memberId,
                            startedEvent.partyId(),
                            startedEvent.partyName(),
                            startedEvent.imageKey(),
                            content,
                            startedEvent.gameBoardId(),
                            startedEvent.eventId(),
                            NotificationOutboxEventType.GAME_STARTED
                    ))
                    .toList();
        }

        throw new IllegalArgumentException("지원하지 않는 게임 이벤트입니다: " + event.getClass().getName());
    }

    private NotificationRequest request(
            Long recipientMemberId,
            Long partyId,
            String partyName,
            String imageKey,
            String content,
            Long gameBoardId,
            UUID eventId,
            NotificationOutboxEventType eventType
    ) {
        return new NotificationRequest(
                NotificationSource.GAME,
                recipientMemberId,
                partyName,
                content,
                imageKey,
                serializeData(gameBoardId),
                destination(gameBoardId),
                new NotificationLegacyCompatibility(partyId, NotificationType.CHANGE),
                eventId,
                eventType
        );
    }

    private String serializeData(Long gameBoardId) {
        try {
            return objectMapper.writeValueAsString(Map.of("gameBoardId", gameBoardId));
        } catch (JsonProcessingException e) {
            log.error("게임 알림 데이터 생성 실패 - gameBoardId: {}", gameBoardId, e);
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DATA);
        }
    }

    private NotificationDestination destination(Long gameBoardId) {
        return new NotificationDestination(
                NotificationResourceType.GAME_BOARD,
                gameBoardId,
                NotificationAction.VIEW
        );
    }
}
