package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.notification.service.NotificationIngressService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyUpdatedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationAcceptedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyJoinRequestApprovedEvent;
import umc.cockple.demo.domain.party.events.PartyRoleChangedEvent;

@Component
@RequiredArgsConstructor
public class PartyNotificationEventListener {

    private final NotificationIngressService notificationIngressService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handlePartyUpdated(PartyUpdatedEvent event) {
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
        notificationIngressService.handle(event);
    }
}
