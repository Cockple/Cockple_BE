package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

public class ExerciseEditDetailDTO {

    @Builder
    public record Response(
            String date,
            String buildingName,
            String roadAddress,
            Double latitude,
            Double longitude,
            String startTime,
            String endTime,
            Integer maxCapacity,
            Boolean allowMemberGuestsInvitation,
            Boolean allowExternalGuests,
            String notice
    ) {
    }
}
