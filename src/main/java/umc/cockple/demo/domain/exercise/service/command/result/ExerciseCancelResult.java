package umc.cockple.demo.domain.exercise.service.command.result;

public record ExerciseCancelResult(
        String memberName,
        Integer currentParticipants
) {
}
