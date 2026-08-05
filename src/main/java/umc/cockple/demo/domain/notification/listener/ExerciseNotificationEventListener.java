package umc.cockple.demo.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseDeletedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseUpdatedEvent;
import umc.cockple.demo.domain.notification.service.NotificationIngressService;

@Component
@RequiredArgsConstructor
public class ExerciseNotificationEventListener {

    private final NotificationIngressService notificationIngressService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleDeleted(ExerciseDeletedEvent event) {
        notificationIngressService.handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleUpdated(ExerciseUpdatedEvent event) {
        notificationIngressService.handle(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationIngressExecutor")
    public void handleAttendanceChanged(ExerciseAttendanceChangedEvent event) {
        notificationIngressService.handle(event);
    }
}
