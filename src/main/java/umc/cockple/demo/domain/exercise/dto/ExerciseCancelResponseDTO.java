package umc.cockple.demo.domain.exercise.dto;

public record ExerciseCancelResponseDTO(
        String memberName,
        Integer cancelledParticipationNumber,
        Integer currentParticipants
) {
}
