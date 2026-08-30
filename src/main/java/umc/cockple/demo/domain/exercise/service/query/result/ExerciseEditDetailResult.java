package umc.cockple.demo.domain.exercise.service.query.result;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExerciseEditDetailResult(
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
