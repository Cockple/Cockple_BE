package umc.cockple.demo.domain.exercise.converter.command;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;

@Component
public class ExerciseLifecycleCommandMapper {

    public ExerciseCreateCommand toCreateCommand(ExerciseCreateDTO.Request request) {
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

    public ExerciseCreateAddressCommand toAddrCreateCommand(ExerciseCreateDTO.Request request) {
        return ExerciseCreateAddressCommand.builder()
                .roadAddress(request.roadAddress())
                .buildingName(request.buildingName())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
    }

    public ExerciseUpdateCommand toUpdateCommand(ExerciseUpdateDTO.Request request) {
        return ExerciseUpdateCommand.builder()
                .date(request.toParsedDate())
                .startTime(request.toParsedStartTime())
                .endTime(request.toParsedEndTime())
                .maxCapacity(request.maxCapacity())
                .partyGuestAccept(request.allowMemberGuestsInvitation())
                .outsideGuestAccept(request.allowExternalGuests())
                .notice(request.notice())
                .build();
    }

    public ExerciseUpdateAddressCommand toAddrUpdateCommand(ExerciseUpdateDTO.Request request) {
        if (request.roadAddress() != null || request.buildingName() != null ||
                request.latitude() != null || request.longitude() != null) {
            return ExerciseUpdateAddressCommand.builder()
                    .roadAddress(request.roadAddress())
                    .buildingName(request.buildingName())
                    .latitude(request.latitude())
                    .longitude(request.longitude())
                    .build();
        }
        return null;
    }

    public ExerciseCreateDTO.Response toCreateResponse(Exercise exercise) {
        return ExerciseCreateDTO.Response.builder()
                .exerciseId(exercise.getId())
                .createdAt(exercise.getCreatedAt())
                .build();
    }

    public ExerciseDeleteDTO.Response toDeleteResponse(Exercise exercise) {
        return ExerciseDeleteDTO.Response.builder()
                .deletedExerciseId(exercise.getId())
                .build();
    }

    public ExerciseUpdateDTO.Response toUpdateResponse(Exercise exercise) {
        return ExerciseUpdateDTO.Response.builder()
                .exerciseId(exercise.getId())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }
}
