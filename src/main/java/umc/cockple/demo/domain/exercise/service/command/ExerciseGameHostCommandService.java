package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGameHostChangeCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGameHostChangeResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseGameHostCommandService {

    private final ExerciseReader exerciseReader;
    private final ExerciseValidator exerciseValidator;

    public ExerciseGameHostChangeResult changeGameHost(
            Long exerciseId,
            Long memberId,
            ExerciseGameHostChangeCommand command) {
        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        exerciseValidator.validateGameHostManagementPermission(exercise, memberId);
        exerciseValidator.validateGameHostCandidate(exercise, command.participantId());

        exercise.changeGameHost(command.participantId());

        return new ExerciseGameHostChangeResult(exercise.getId(), exercise.getGameHostId());
    }
}
