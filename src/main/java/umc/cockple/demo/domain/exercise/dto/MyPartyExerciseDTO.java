package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MyPartyExerciseDTO {

    @Builder
    public record Response(
            Integer totalExercises,
            List<Exercises> exercises
    ) {
    }

    @Builder
    public record Exercises(
            Long exerciseId,
            Long partyId,
            String partyName,
            String buildingName,
            LocalDate date,
            String dayOfWeek,
            LocalTime startTime,
            String profileImageUrl
    ){
    }
}
