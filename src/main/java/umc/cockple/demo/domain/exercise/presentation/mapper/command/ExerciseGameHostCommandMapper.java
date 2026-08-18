package umc.cockple.demo.domain.exercise.presentation.mapper.command;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGameHostChangeCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGameHostChangeResult;

@Component
public class ExerciseGameHostCommandMapper {

    public ExerciseGameHostChangeCommand toChangeCommand(
            ExerciseGameHostDTO.ChangeRequest request) {
        return new ExerciseGameHostChangeCommand(request.participantId());
    }

    public ExerciseGameHostDTO.ChangeResponse toChangeResponse(
            ExerciseGameHostChangeResult result) {
        return ExerciseGameHostDTO.ChangeResponse.builder()
                .exerciseId(result.exerciseId())
                .participantId(result.participantId())
                .build();
    }
}
