package umc.cockple.demo.domain.notification.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyInfoChangedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationAcceptedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyJoinRequestApprovedEvent;
import umc.cockple.demo.domain.party.events.PartyRoleChangedEvent;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartyNotificationStrategy implements NotificationEventStrategy {

    private final NotificationMessageGenerator notificationMessageGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Object event) {
        return event instanceof PartyInfoChangedEvent
                || event instanceof PartyDeletedEvent
                || event instanceof PartyJoinRequestApprovedEvent
                || event instanceof PartyRoleChangedEvent
                || event instanceof PartyInvitationCreatedEvent
                || event instanceof PartyInvitationAcceptedEvent;
    }

    @Override
    public List<NotificationRequest> convert(Object event) {
        if (event instanceof PartyInfoChangedEvent partyInfoChangedEvent) {
            return List.of(request(
                    partyInfoChangedEvent.recipientMemberId(),
                    partyInfoChangedEvent.partyId(),
                    partyInfoChangedEvent.partyName(),
                    partyInfoChangedEvent.imageKey(),
                    notificationMessageGenerator.generatePartyInfoChangedMessage(),
                    destination(partyInfoChangedEvent.partyId()),
                    NotificationType.CHANGE,
                    Map.of()
            ));
        }

        if (event instanceof PartyDeletedEvent partyDeletedEvent) {
            return List.of(request(
                    partyDeletedEvent.deletedByMemberId(),
                    partyDeletedEvent.partyId(),
                    partyDeletedEvent.partyName(),
                    partyDeletedEvent.imageKey(),
                    notificationMessageGenerator.generatePartyDeletedMessage(),
                    null,
                    NotificationType.SIMPLE,
                    Map.of()
            ));
        }

        if (event instanceof PartyJoinRequestApprovedEvent approvedEvent) {
            return List.of(request(
                    approvedEvent.recipientMemberId(),
                    approvedEvent.partyId(),
                    approvedEvent.partyName(),
                    approvedEvent.imageKey(),
                    notificationMessageGenerator.generateJoinRequestApprovedMessage(),
                    destination(approvedEvent.partyId()),
                    NotificationType.CHANGE,
                    Map.of()
            ));
        }

        if (event instanceof PartyRoleChangedEvent roleChangedEvent) {
            String content = roleChangedEvent.action() == PartyRoleChangedEvent.RoleChangeAction.SUBOWNER_ASSIGNED
                    ? notificationMessageGenerator.generateSubOwnerAssignedMessage(roleChangedEvent.subjectNickname())
                    : notificationMessageGenerator.generateSubOwnerReleasedMessage(roleChangedEvent.subjectNickname());

            return roleChangedEvent.recipientMemberIds().stream()
                    .map(memberId -> request(
                            memberId,
                            roleChangedEvent.partyId(),
                            roleChangedEvent.partyName(),
                            roleChangedEvent.imageKey(),
                            content,
                            destination(roleChangedEvent.partyId()),
                            NotificationType.SIMPLE,
                            Map.of()
                    ))
                    .toList();
        }

        if (event instanceof PartyInvitationCreatedEvent invitationCreatedEvent) {
            return List.of(request(
                    invitationCreatedEvent.inviteeId(),
                    invitationCreatedEvent.partyId(),
                    "새로운 모임",
                    invitationCreatedEvent.imageKey(),
                    notificationMessageGenerator.generateInviteMessage(invitationCreatedEvent.partyName()),
                    destination(
                            NotificationResourceType.PARTY_INVITATION,
                            invitationCreatedEvent.invitationId(),
                            NotificationAction.RESPOND
                    ),
                    NotificationType.INVITE,
                    Map.of("invitationId", invitationCreatedEvent.invitationId())
            ));
        }

        if (event instanceof PartyInvitationAcceptedEvent invitationAcceptedEvent) {
            return List.of(request(
                    invitationAcceptedEvent.inviterId(),
                    invitationAcceptedEvent.partyId(),
                    invitationAcceptedEvent.partyName(),
                    invitationAcceptedEvent.imageKey(),
                    notificationMessageGenerator.generateInviteApprovedMessage(
                            invitationAcceptedEvent.inviteeNickname()),
                    destination(invitationAcceptedEvent.partyId()),
                    NotificationType.SIMPLE,
                    Map.of()
            ));
        }

        throw new IllegalArgumentException("지원하지 않는 모임 이벤트입니다: " + event.getClass().getName());
    }

    private NotificationRequest request(
            Long recipientMemberId,
            Long partyId,
            String title,
            String imageKey,
            String content,
            NotificationDestination destination,
            NotificationType legacyType,
            Map<String, Object> data
    ) {
        return new NotificationRequest(
                NotificationSource.PARTY,
                recipientMemberId,
                title,
                content,
                imageKey,
                serialize(data),
                destination,
                new NotificationLegacyCompatibility(partyId, legacyType)
        );
    }

    private String serialize(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("모임 알림 데이터 생성 실패", e);
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DATA);
        }
    }

    private NotificationDestination destination(Long partyId) {
        return destination(NotificationResourceType.PARTY, partyId, NotificationAction.VIEW);
    }

    private NotificationDestination destination(
            NotificationResourceType resourceType,
            Long resourceId,
            NotificationAction action
    ) {
        return new NotificationDestination(resourceType, resourceId, action);
    }
}
