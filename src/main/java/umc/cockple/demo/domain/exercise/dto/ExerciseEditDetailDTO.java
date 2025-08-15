package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

public class ExerciseEditDetailDTO {

    @Builder
    public record Response(
            LocalDate date,
            String buildingName,
            String roadAddress,
            Double latitude,
            Double longitude,
            LocalTime startTime,
            LocalTime endTime,
            Integer maxCapacity,
            Boolean allowMemberGuestsInvitation,
            Boolean allowExternalGuests,
            String notice
    ) {
    }
}
