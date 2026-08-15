package umc.cockple.demo.domain.exercise.presentation.mapper.command;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.participation.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCancelByManagerCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseJoinResult;

@Component
public class ExerciseParticipationCommandMapper {

    public ExerciseCancelByManagerCommand toCancelByManagerCommand(ExerciseCancelDTO.ByManagerRequest request) {
        return new ExerciseCancelByManagerCommand(request.isGuest());
    }

    public ExerciseJoinDTO.Response toJoinResponse(ExerciseJoinResult result) {
        return ExerciseJoinDTO.Response.builder()
                .participantId(result.participantId())
                .joinedAt(result.joinedAt())
                .currentParticipants(result.currentParticipants())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(ExerciseCancelResult result) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(result.memberName())
                .currentParticipants(result.currentParticipants())
                .build();
    }
}
