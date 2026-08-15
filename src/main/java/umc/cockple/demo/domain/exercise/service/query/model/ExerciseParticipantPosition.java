package umc.cockple.demo.domain.exercise.service.query.model;

public record ExerciseParticipantPosition(
        ExerciseParticipantSnapshot participant,
        int participantNumber,
        boolean waiting
) {
}
