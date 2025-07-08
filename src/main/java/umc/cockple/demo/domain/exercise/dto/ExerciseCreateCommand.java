package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record ExerciseCreateCommand(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer maxCapacity,
        Boolean partyGuestAccept,
        Boolean outsideGuestAccept,
        String notice
) {
}
