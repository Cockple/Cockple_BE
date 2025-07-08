package umc.cockple.demo.domain.exercise.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseAddrCreateCommand;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateRequestDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateResponseDTO;

@Component
@RequiredArgsConstructor
public class ExerciseConverter {

    public ExerciseCreateCommand toCreateCommand(ExerciseCreateRequestDTO request) {
        return ExerciseCreateCommand.builder()
                .date(request.toParsedDate())
                .startTime(request.toParsedStartTime())
                .endTime(request.toParsedEndTime())
                .maxCapacity(request.maxCapacity())
                .partyGuestAccept(request.allowMemberGuestsInvitation())
                .outsideGuestAccept(request.allowExternalGuests())
                .notice(request.notice())
                .build();
    }

    public ExerciseAddrCreateCommand toAddrCreateCommand(ExerciseCreateRequestDTO request) {
        return ExerciseAddrCreateCommand.builder()
                .roadAddress(request.roadAddress())
                .buildingName(request.buildingName())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
    }

    public ExerciseCreateResponseDTO toCreateResponseDTO(Exercise exercise) {
        return ExerciseCreateResponseDTO.builder()
                .exerciseId(exercise.getId())
                .createdAt(exercise.getCreatedAt())
                .build();
    }
}
