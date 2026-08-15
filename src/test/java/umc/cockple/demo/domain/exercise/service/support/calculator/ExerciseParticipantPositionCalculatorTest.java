package umc.cockple.demo.domain.exercise.service.support.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantPosition;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("ExerciseParticipantPositionCalculator")
class ExerciseParticipantPositionCalculatorTest {

    private final ExerciseParticipantPositionCalculator calculator =
            new ExerciseParticipantPositionCalculator();

    @Test
    @DisplayName("참여 시각과 정원에 따라 참가자와 대기자의 번호를 계산한다")
    void calculateParticipantPositions() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 15, 10, 0);
        List<ExerciseParticipantSnapshot> participants = List.of(
                participant(1L, baseTime.plusMinutes(2)),
                participant(2L, baseTime),
                participant(3L, baseTime.plusMinutes(4))
        );

        List<ExerciseParticipantPosition> result = calculator.calculate(participants, 2);

        assertThat(result)
                .extracting(
                        position -> position.participant().participantId(),
                        ExerciseParticipantPosition::participantNumber,
                        ExerciseParticipantPosition::waiting)
                .containsExactly(
                        tuple(2L, 1, false),
                        tuple(1L, 2, false),
                        tuple(3L, 1, true)
                );
    }

    private ExerciseParticipantSnapshot participant(Long id, LocalDateTime joinedAt) {
        return new ExerciseParticipantSnapshot(
                id,
                null,
                "참여자" + id,
                Gender.MALE,
                Level.A,
                ExerciseMemberShipStatus.PARTY_MEMBER,
                null,
                null,
                joinedAt,
                false
        );
    }
}
