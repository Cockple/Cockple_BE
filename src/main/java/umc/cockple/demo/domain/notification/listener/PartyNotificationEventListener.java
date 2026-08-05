package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.notification.command.NotificationCreateCommand;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.strategy.NotificationStrategyRegistry;
import umc.cockple.demo.domain.notification.service.NotificationV2CommandService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyInfoChangedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationAcceptedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyJoinRequestApprovedEvent;
import umc.cockple.demo.domain.party.events.PartyRoleChangedEvent;

@Component
@RequiredArgsConstructor
public class PartyNotificationEventListener {

    private final NotificationV2CommandService notificationV2CommandService;
    private final NotificationStrategyRegistry notificationStrategyRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handlePartyInfoChanged(PartyInfoChangedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handlePartyDeleted(PartyDeletedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleJoinRequestApproved(PartyJoinRequestApprovedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleRoleChanged(PartyRoleChangedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleInvitationCreated(PartyInvitationCreatedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleInvitationAccepted(PartyInvitationAcceptedEvent event) {
        handle(event);
    }

    private void handle(Object event) {
        notificationStrategyRegistry.convert(event)
                .forEach(this::createNotification);
    }

    private void createNotification(NotificationRequest request) {
        notificationV2CommandService.createNotification(
                new NotificationCreateCommand(
                        request.recipientMemberId(),
                        request.title(),
                        request.content(),
                        request.imageKey(),
                        request.data(),
                        request.destination()
                ),
                request.legacyCompatibility()
        );
    }
}
