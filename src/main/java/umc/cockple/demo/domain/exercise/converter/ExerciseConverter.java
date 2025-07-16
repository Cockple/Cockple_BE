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

    public ExerciseCreateDTO.Command toCreateCommand(ExerciseCreateDTO.Request request) {
        return ExerciseCreateDTO.Command.builder()
                .date(request.toParsedDate())
                .startTime(request.toParsedStartTime())
                .endTime(request.toParsedEndTime())
                .maxCapacity(request.maxCapacity())
                .partyGuestAccept(request.allowMemberGuestsInvitation())
                .outsideGuestAccept(request.allowExternalGuests())
                .notice(request.notice())
                .build();
    }

    public ExerciseCreateDTO.AddrCommand toAddrCreateCommand(ExerciseCreateDTO.Request request) {
        return ExerciseCreateDTO.AddrCommand.builder()
                .roadAddress(request.roadAddress())
                .buildingName(request.buildingName())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
    }

    public ExerciseCreateDTO.Response toCreateResponseDTO(Exercise exercise) {
        return ExerciseCreateDTO.Response.builder()
                .exerciseId(exercise.getId())
                .createdAt(exercise.getCreatedAt())
                .build();
    }

    public ExerciseJoinDTO.Response toJoinResponseDTO(MemberExercise memberExercise, Exercise exercise) {
        return ExerciseJoinDTO.Response.builder()
                .participantId(memberExercise.getId())
                .participantNumber(memberExercise.getParticipantNum())
                .joinedAt(memberExercise.getJoinedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseGuestInviteDTO.Command toGuestInviteCommand(ExerciseGuestInviteDTO.Request request, Long inviterId) {
        return ExerciseGuestInviteDTO.Command.builder()
                .guestName(request.guestName())
                .gender(request.toParsedGender())
                .level(request.toParsedLevel())
                .inviterId(inviterId)
                .build();
    }

    public ExerciseGuestInviteDTO.Response toGuestInviteResponseDTO(Guest guest, Exercise exercise) {
        return ExerciseGuestInviteDTO.Response.builder()
                .guestId(guest.getId())
                .participantNumber(guest.getParticipantNum())
                .invitedAt(guest.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponseDTO(Exercise exercise, Member member, Integer participantNumber) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(member.getMemberName())
                .cancelledParticipationNumber(participantNumber)
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponseDTO(Exercise exercise, Guest guest, Integer participantNumber) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(guest.getGuestName())
                .cancelledParticipationNumber(participantNumber)
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseDeleteDTO.Response toDeleteResponseDTO(Exercise exercise) {
        return ExerciseDeleteDTO.Response.builder()
                .deletedExerciseId(exercise.getId())
                .build();
    }

    public ExerciseUpdateDTO.Command toUpdateCommand(ExerciseUpdateDTO.Request request) {
        return ExerciseUpdateDTO.Command.builder()
                .date(request.toParsedDate())
                .startTime(request.toParsedStartTime())
                .endTime(request.toParsedEndTime())
                .maxCapacity(request.maxCapacity())
                .notice(request.notice())
                .build();
    }

    public ExerciseUpdateDTO.AddrCommand toAddrUpdateCommand(ExerciseUpdateDTO.Request request) {
        if (request.roadAddress() != null || request.buildingName() != null ||
                request.latitude() != null || request.longitude() != null) {
            return ExerciseUpdateDTO.AddrCommand.builder()
                    .roadAddress(request.roadAddress())
                    .buildingName(request.buildingName())
                    .latitude(request.latitude())
                    .longitude(request.longitude())
                    .build();
        }
        return null;
    }

    public ExerciseUpdateDTO.Response toUpdateResponseDTO(Exercise exercise) {
        return ExerciseUpdateDTO.Response.builder()
                .exerciseId(exercise.getId())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }
}
