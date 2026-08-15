package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.lifecycle.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseEditDetailResult;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExerciseLifecycleQueryMapper {

    private final ExerciseParticipantInfoQueryMapper participantInfoMapper;

    public ExerciseDetailDTO.Response toDetailResponse(ExerciseDetailResult result) {
        return ExerciseDetailDTO.Response.builder()
                .isManager(result.isManager())
                .info(toExerciseInfo(result.info()))
                .participants(toParticipantGroup(result.participants()))
                .waiting(toWaitingGroup(result.waiting()))
                .build();
    }

    public ExerciseEditDetailDTO.Response toEditDetailResponse(ExerciseEditDetailResult result) {
        return ExerciseEditDetailDTO.Response.builder()
                .date(result.date())
                .buildingName(result.buildingName())
                .roadAddress(result.roadAddress())
                .latitude(result.latitude())
                .longitude(result.longitude())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .maxCapacity(result.maxCapacity())
                .allowMemberGuestsInvitation(result.allowMemberGuestsInvitation())
                .allowExternalGuests(result.allowExternalGuests())
                .notice(result.notice())
                .build();
    }

    private ExerciseDetailDTO.ExerciseInfo toExerciseInfo(ExerciseDetailResult.ExerciseInfo result) {
        return ExerciseDetailDTO.ExerciseInfo.builder()
                .notice(result.notice())
                .buildingName(result.buildingName())
                .location(result.location())
                .build();
    }

    private ExerciseDetailDTO.ParticipantGroup toParticipantGroup(
            ExerciseDetailResult.ParticipantGroup result) {
        return ExerciseDetailDTO.ParticipantGroup.builder()
                .currentParticipantCount(result.currentParticipantCount())
                .totalCount(result.totalCount())
                .manCount(result.manCount())
                .womenCount(result.womenCount())
                .list(toParticipantInfos(result.list()))
                .build();
    }

    private ExerciseDetailDTO.WaitingGroup toWaitingGroup(ExerciseDetailResult.WaitingGroup result) {
        return ExerciseDetailDTO.WaitingGroup.builder()
                .currentWaitingCount(result.currentWaitingCount())
                .manCount(result.manCount())
                .womenCount(result.womenCount())
                .list(toParticipantInfos(result.list()))
                .build();
    }

    private List<ExerciseDetailDTO.ParticipantInfo> toParticipantInfos(
            List<ExerciseDetailResult.ParticipantInfo> results) {
        return results.stream()
                .map(participantInfoMapper::toParticipantInfo)
                .toList();
    }
}
