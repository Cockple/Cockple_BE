package umc.cockple.demo.domain.exercise.converter.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseEditDetailDTO;

@Component
public class ExerciseLifecycleQueryMapper {

    public ExerciseDetailDTO.Response toDetailResponse(
            boolean isManager,
            ExerciseDetailDTO.ExerciseInfo exerciseInfo,
            ExerciseDetailDTO.ParticipantGroup participantGroup,
            ExerciseDetailDTO.WaitingGroup waitingGroup) {

        return ExerciseDetailDTO.Response.builder()
                .isManager(isManager)
                .info(exerciseInfo)
                .participants(participantGroup)
                .waiting(waitingGroup)
                .build();
    }

    public ExerciseEditDetailDTO.Response toEditDetailResponse(Exercise exercise) {
        ExerciseAddr addr = exercise.getExerciseAddr();

        return ExerciseEditDetailDTO.Response.builder()
                .date(exercise.getDate())
                .buildingName(addr.getBuildingName())
                .roadAddress(addr.getStreetAddr())
                .latitude(addr.getLatitude())
                .longitude(addr.getLongitude())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .maxCapacity(exercise.getMaxCapacity())
                .allowMemberGuestsInvitation(exercise.getPartyGuestAccept())
                .allowExternalGuests(exercise.getOutsideGuestAccept())
                .notice(exercise.getNotice())
                .build();
    }
}
