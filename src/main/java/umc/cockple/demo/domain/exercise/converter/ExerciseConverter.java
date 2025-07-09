package umc.cockple.demo.domain.exercise.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;

import java.time.LocalDateTime;

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

    public ExerciseJoinResponseDTO toJoinResponseDTO(MemberExercise memberExercise, Exercise exercise) {
        return ExerciseJoinResponseDTO.builder()
                .participantId(memberExercise.getId())
                .participantNumber(memberExercise.getParticipantNum())
                .joinedAt(memberExercise.getJoinedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }
}
