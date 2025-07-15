package umc.cockple.demo.domain.exercise.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;

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

    public GuestInviteCommand toGuestInviteCommand(GuestInviteRequestDTO request, Long inviterId) {
        return GuestInviteCommand.builder()
                .guestName(request.guestName())
                .gender(request.toParsedGender())
                .level(request.toParsedLevel())
                .inviterId(inviterId)
                .build();
    }

    public GuestInviteResponseDTO toGuestInviteResponseDTO(Guest guest, Exercise exercise) {
        return GuestInviteResponseDTO.builder()
                .guestId(guest.getId())
                .participantNumber(guest.getParticipantNum())
                .invitedAt(guest.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelResponseDTO toCancelResponseDTO(Exercise exercise, Member member, Integer participantNumber) {
        return ExerciseCancelResponseDTO.builder()
                .memberName(member.getMemberName())
                .cancelledParticipationNumber(participantNumber)
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelResponseDTO toCancelResponseDTO(Exercise exercise, Guest guest, Integer participantNumber) {
        return ExerciseCancelResponseDTO.builder()
                .memberName(guest.getGuestName())
                .cancelledParticipationNumber(participantNumber)
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseDeleteResponseDTO toDeleteResponseDTO(Exercise exercise) {
        return ExerciseDeleteResponseDTO.builder()
                .deletedExerciseId(exercise.getId())
                .build();
    }
}
