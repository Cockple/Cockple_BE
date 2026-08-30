package umc.cockple.demo.domain.notification.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseDeletedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseUpdatedEvent;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.service.NotificationMessageGenerator;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseNotificationStrategyTest {

    // 운영 환경의 자동설정 ObjectMapper와 동일하게 LocalDate 직렬화를 지원하도록 JavaTimeModule을 등록한다.
    private final ExerciseNotificationStrategy strategy =
            new ExerciseNotificationStrategy(
                    new NotificationMessageGenerator(),
                    new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    @DisplayName("운동 삭제 알림은 수신자별로 생성되고 destination이 없다")
    void convertsDeletedExerciseNotification() {
        List<NotificationRequest> requests = strategy.convert(
                ExerciseDeletedEvent.deleted(
                        100L, 10L, "모임", "image-key",
                        LocalDate.of(2099, 12, 31), List.of(20L, 30L)));

        assertThat(requests).extracting(NotificationRequest::recipientMemberId)
                .containsExactly(20L, 30L);
        assertThat(requests).allMatch(request -> request.destination() == null);
        assertThat(requests.get(0).content()).contains("12.31");
    }

    @Test
    @DisplayName("운동 수정 알림은 운동 화면 destination을 가진다")
    void convertsUpdatedExerciseNotification() {
        List<NotificationRequest> requests = strategy.convert(
                ExerciseUpdatedEvent.updated(
                        100L, 10L, "모임", "image-key",
                        LocalDate.of(2099, 12, 31), List.of(20L)));

        assertThat(requests.get(0).destination().resourceType())
                .isEqualTo(NotificationResourceType.EXERCISE);
        assertThat(requests.get(0).destination().resourceId()).isEqualTo(100L);
        assertThat(requests.get(0).destination().action()).isEqualTo(NotificationAction.VIEW);
    }

    @Test
    @DisplayName("운동 참석 변경 알림은 수신자별로 생성한다")
    void convertsAttendanceChangedNotification() {
        List<NotificationRequest> requests = strategy.convert(
                ExerciseAttendanceChangedEvent.changed(
                        100L, 10L, "모임", "image-key",
                        LocalDate.of(2099, 12, 31), 20L, List.of(30L, 40L)));

        assertThat(requests).extracting(NotificationRequest::recipientMemberId)
                .containsExactly(30L, 40L);
        assertThat(requests.get(0).destination().resourceType())
                .isEqualTo(NotificationResourceType.EXERCISE);
    }
}
