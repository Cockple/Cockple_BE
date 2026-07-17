package umc.cockple.demo.domain.exercise.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ExerciseGuestMapper {

    public ExerciseGuestInviteDTO.Command toGuestInviteCommand(ExerciseGuestInviteDTO.Request request, Long inviterId) {
        return ExerciseGuestInviteDTO.Command.builder()
                .guestName(request.guestName())
                .gender(request.toParsedGender())
                .level(request.toParsedLevel())
                .inviterId(inviterId)
                .build();
    }

    public ExerciseGuestInviteDTO.Response toGuestInviteResponse(Guest guest, Exercise exercise) {
        return ExerciseGuestInviteDTO.Response.builder()
                .guestId(guest.getId())
                .invitedAt(guest.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(Exercise exercise, Guest guest) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(guest.getGuestName())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseMyGuestListDTO.Response toEmptyGuestListResponse() {
        return ExerciseMyGuestListDTO.Response.builder()
                .totalCount(0)
                .maleCount(0)
                .femaleCount(0)
                .list(Collections.emptyList())
                .build();
    }

    public ExerciseMyGuestListDTO.Response toMyGuestListResponse(
            ExerciseMyGuestListDTO.GuestStatistics statistics,
            List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList) {

        return ExerciseMyGuestListDTO.Response.builder()
                .totalCount(statistics.totalCount())
                .maleCount(statistics.maleCount())
                .femaleCount(statistics.femaleCount())
                .list(guestInfoList)
                .build();
    }

    public ExerciseMyGuestListDTO.GuestInfo toGuestInfo(
            Guest guest,
            Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestStatusMap,
            String inviterName) {

        ExerciseMyGuestListDTO.GuestGroups guestGroup = guestStatusMap.get(guest.getId());

        return ExerciseMyGuestListDTO.GuestInfo.builder()
                .guestId(guest.getId())
                .isWaiting(guestGroup.isWaiting())
                .participantNumber(guestGroup.participantNumber())
                .name(guest.getGuestName())
                .gender(guest.getGender())
                .level(guest.getLevel())
                .inviterName(inviterName)
                .build();
    }
}
