package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

@Builder
public record ExerciseAddrUpdateCommand(
        String roadAddress,
        String buildingName,
        Double latitude,
        Double longitude
) {
}
