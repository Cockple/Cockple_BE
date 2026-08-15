package umc.cockple.demo.domain.exercise.service.support.calculator;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantPosition;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class ExerciseParticipantPositionCalculator {

    public List<ExerciseParticipantPosition> calculate(
            List<ExerciseParticipantSnapshot> participants,
            int maxCapacity) {

        List<ExerciseParticipantSnapshot> sortedParticipants = participants.stream()
                .sorted(Comparator.comparing(ExerciseParticipantSnapshot::joinedAt))
                .toList();

        return IntStream.range(0, sortedParticipants.size())
                .mapToObj(index -> toPosition(sortedParticipants.get(index), index, maxCapacity))
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
