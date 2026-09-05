package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseDeletedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseUpdatedEvent;
import umc.cockple.demo.domain.game.events.GameHostAssignedEvent;
import umc.cockple.demo.domain.game.events.GameStartedEvent;
import umc.cockple.demo.domain.notification.service.outbox.NotificationOutboxService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationAcceptedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyJoinRequestApprovedEvent;
import umc.cockple.demo.domain.party.events.PartyRoleChangedEvent;
import umc.cockple.demo.domain.party.events.PartyUpdatedEvent;

@Component
@RequiredArgsConstructor
public class NotificationOutboxEventListener {

    private final NotificationOutboxService notificationOutboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePartyUpdated(PartyUpdatedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePartyDeleted(PartyDeletedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleInvitationCreated(PartyInvitationCreatedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleInvitationAccepted(PartyInvitationAcceptedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleJoinRequestApproved(PartyJoinRequestApprovedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleRoleChanged(PartyRoleChangedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleExerciseDeleted(ExerciseDeletedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleExerciseUpdated(ExerciseUpdatedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleExerciseAttendanceChanged(ExerciseAttendanceChangedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleGameStarted(GameStartedEvent event) {
        record(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleGameHostAssigned(GameHostAssignedEvent event) {
        record(event);
    }

    private void record(Object event) {
        notificationOutboxService.record(event);
    }
}
