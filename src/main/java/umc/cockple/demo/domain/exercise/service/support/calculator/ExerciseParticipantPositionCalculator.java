package umc.cockple.demo.domain.exercise.service.support.calculator;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantPosition;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

@Component
public class ExerciseParticipantPositionCalculator {

    public List<ExerciseParticipantPosition> calculate(
            List<ExerciseParticipantSnapshot> participants,
            int maxCapacity) {

        List<ExerciseParticipantSnapshot> sortedParticipants = sortByJoinedAt(
                participants, ExerciseParticipantSnapshot::joinedAt);

        return IntStream.range(0, sortedParticipants.size())
                .mapToObj(index -> toPosition(sortedParticipants.get(index), index, maxCapacity))
                .toList();
    }

    public <T> List<T> sortByJoinedAt(List<T> participants, Function<T, LocalDateTime> joinedAt) {
        return participants.stream()
                .sorted(Comparator.comparing(joinedAt))
                .toList();
    }

    private ExerciseParticipantPosition toPosition(
            ExerciseParticipantSnapshot participant,
            int index,
            int maxCapacity) {

        boolean waiting = index >= maxCapacity;
        int participantNumber = waiting ? index - maxCapacity + 1 : index + 1;

        return new ExerciseParticipantPosition(participant, participantNumber, waiting);
    }
}
