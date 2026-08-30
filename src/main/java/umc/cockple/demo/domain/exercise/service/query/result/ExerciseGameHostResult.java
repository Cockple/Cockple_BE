package umc.cockple.demo.domain.exercise.service.query.result;

import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.List;

public record ExerciseGameHostResult(
        int totalCount,
        List<Participant> participants
) {

    public record Participant(
            Long participantId,
            String profileImageUrl,
            Role partyPosition,
            boolean gameHost,
            String name,
            Gender gender,
            Level level,
            LocalDate lastExerciseDate
    ) {
    }
}
