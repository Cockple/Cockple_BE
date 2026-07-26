package umc.cockple.demo.domain.exercise.service.command.model;

import lombok.Builder;

@Builder
public record ExerciseUpdateAddressCommand(
        String roadAddress,
        String buildingName,
        Double latitude,
        Double longitude
) {
}
