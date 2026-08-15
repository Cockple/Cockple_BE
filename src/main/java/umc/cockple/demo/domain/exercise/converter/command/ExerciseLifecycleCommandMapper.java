package umc.cockple.demo.domain.exercise.converter.command;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCreateResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseDeleteResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseUpdateResult;

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

    public ExerciseCreateDTO.Response toCreateResponse(ExerciseCreateResult result) {
        return ExerciseCreateDTO.Response.builder()
                .exerciseId(result.exerciseId())
                .createdAt(result.createdAt())
                .build();
    }

    public ExerciseDeleteDTO.Response toDeleteResponse(ExerciseDeleteResult result) {
        return ExerciseDeleteDTO.Response.builder()
                .deletedExerciseId(result.deletedExerciseId())
                .build();
    }

    public ExerciseUpdateDTO.Response toUpdateResponse(ExerciseUpdateResult result) {
        return ExerciseUpdateDTO.Response.builder()
                .exerciseId(result.exerciseId())
                .updatedAt(result.updatedAt())
                .build();
    }
}
