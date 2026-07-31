package umc.cockple.demo.domain.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.dto.NotificationCreateCommand;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.service.NotificationMessageGenerator;
import umc.cockple.demo.domain.notification.service.NotificationV2CommandService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyInfoChangedEvent;
import umc.cockple.demo.domain.party.events.PartyJoinRequestApprovedEvent;
import umc.cockple.demo.domain.party.events.PartyRoleChangedEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartyNotificationEventListener {

    private final NotificationV2CommandService notificationV2CommandService;
    private final NotificationMessageGenerator notificationMessageGenerator;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handlePartyInfoChanged(PartyInfoChangedEvent event) {
        createNotification(
                event.recipientMemberId(),
                event.partyId(),
                event.partyName(),
                event.imageKey(),
                notificationMessageGenerator.generatePartyInfoChangedMessage(),
                destination(event.partyId()),
                Map.of()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handlePartyDeleted(PartyDeletedEvent event) {
        createNotification(
                event.deletedByMemberId(),
                event.partyId(),
                event.partyName(),
                event.imageKey(),
                notificationMessageGenerator.generatePartyDeletedMessage(),
                null,
                Map.of()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handleJoinRequestApproved(PartyJoinRequestApprovedEvent event) {
        createNotification(
                event.recipientMemberId(),
                event.partyId(),
                event.partyName(),
                event.imageKey(),
                notificationMessageGenerator.generateJoinRequestApprovedMessage(),
                destination(event.partyId()),
                Map.of()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handleRoleChanged(PartyRoleChangedEvent event) {
        String content = event.action() == PartyRoleChangedEvent.RoleChangeAction.SUBOWNER_ASSIGNED
                ? notificationMessageGenerator.generateSubOwnerAssignedMessage(event.subjectNickname())
                : notificationMessageGenerator.generateSubOwnerReleasedMessage(event.subjectNickname());

        event.recipientMemberIds().forEach(memberId -> createNotification(
                memberId,
                event.partyId(),
                event.partyName(),
                event.imageKey(),
                content,
                destination(event.partyId()),
                Map.of()
        ));
    }

    private void createNotification(
            Long recipientMemberId,
            Long partyId,
            String partyName,
            String imageKey,
            String content,
            NotificationDestination destination,
            Map<String, Object> data
    ) {
        try {
            notificationV2CommandService.createNotification(new NotificationCreateCommand(
                    recipientMemberId,
                    partyName,
                    content,
                    imageKey,
                    objectMapper.writeValueAsString(data),
                    destination
            ));
        } catch (JsonProcessingException e) {
            log.error("모임 알림 데이터 생성 실패 - partyId: {}, memberId: {}", partyId, recipientMemberId, e);
        }
    }

    private NotificationDestination destination(Long partyId) {
        return new NotificationDestination(
                NotificationResourceType.PARTY,
                partyId,
                NotificationAction.VIEW
        );
    }
}
