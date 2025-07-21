package umc.cockple.demo.domain.exercise.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                .joinedAt(memberExercise.getCreatedAt())
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
                .invitedAt(guest.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponseDTO(Exercise exercise, Member member) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(member.getMemberName())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponseDTO(Exercise exercise, Guest guest) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(guest.getGuestName())
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

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfo(MemberExercise memberParticipant, Map<Long, Role> memberRoles) {
        Member member = memberParticipant.getMember();
        Role role = memberRoles.get(member.getId());

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(memberParticipant.getId())
                .participantNumber(0)
                .imgUrl(member.getProfileImg() != null ? member.getProfileImg().getImgUrl() : null)
                .name(member.getMemberName())
                .gender(member.getGender().name())
                .level(member.getLevel().name())
                .participantType(memberParticipant.getExerciseMemberShipStatus().name())
                .partyPosition(role.name())
                .inviterName(null)
                .joinedAt(memberParticipant.getCreatedAt())
                .build();
    }

    public ExerciseDetailDTO.ParticipantInfo toExeternalParticipantInfo(MemberExercise memberParticipant) {
        Member member = memberParticipant.getMember();

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(memberParticipant.getId())
                .participantNumber(0)
                .imgUrl(member.getProfileImg() != null ? member.getProfileImg().getImgUrl() : null)
                .name(member.getMemberName())
                .gender(member.getGender().name())
                .level(member.getLevel().name())
                .participantType(memberParticipant.getExerciseMemberShipStatus().name())
                .partyPosition(null)
                .inviterName(null)
                .joinedAt(memberParticipant.getCreatedAt())
                .build();
    }

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfo(Guest guest, String inviterName) {

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(guest.getId())
                .participantNumber(0)
                .imgUrl(null)
                .name(guest.getGuestName())
                .gender(guest.getGender().name())
                .level(guest.getLevel().name())
                .participantType(guest.getExerciseMemberShipStatus().name())
                .partyPosition(null)
                .inviterName(inviterName)
                .joinedAt(guest.getCreatedAt())
                .build();
    }

    public ExerciseDetailDTO.Response toDetailResponseDTO(
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

    public PartyExerciseCalendarDTO.Response toCalenderResponse(
            List<Exercise> exercises, LocalDate start, LocalDate end, Boolean isMember, Party party) {

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = groupExerciseByWeek(exercises, party, start, end);

        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .isMember(isMember)
                .partyName(party.getPartyName())
                .weeks(weeks)
                .build();
    }

    private List<PartyExerciseCalendarDTO.WeeklyExercises> groupExerciseByWeek(List<Exercise> exercises, Party party, LocalDate start, LocalDate end) {

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        LocalDate weekStart = start.minusDays(start.getDayOfWeek().getValue() - 1);

        while (!weekStart.isAfter(end)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            LocalDate currentWeekStart = weekStart;
            List<Exercise> weekExercises = exercises.stream()
                    .filter(exercise -> {
                        LocalDate exerciseDate = exercise.getDate();
                        return (!exerciseDate.isBefore(currentWeekStart) && !exerciseDate.isAfter(weekEnd));
                    })
                    .toList();

            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = weekExercises.stream()
                    .map((Exercise exercise) -> toCalendarItem(exercise, party))
                    .toList();

            weeks.add(PartyExerciseCalendarDTO.WeeklyExercises.builder()
                    .weekStartDate(currentWeekStart)
                    .weekEndDate(weekEnd)
                    .exercises(exerciseItems)
                    .build());

            weekStart = currentWeekStart.plusWeeks(1);
        }

        return weeks;
    }

    private PartyExerciseCalendarDTO.ExerciseCalendarItem toCalendarItem(Exercise exercise, Party party) {
        return PartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .femaleLevel(getLevelList(party, Gender.FEMALE))
                .maleLevel(getLevelList(party, Gender.MALE))
                .currentParticipants(exercise.getNowCapacity())
                .maxCapacity(exercise.getMaxCapacity())
                .build();
    }

    private List<String> getLevelList(Party party, Gender gender) {
        List<String> levelList = party.getLevels().stream()
                .filter(l -> l.getGender() == gender)
                .map(l -> l.getLevel().getKoreanName())
                .toList();

        return levelList.isEmpty() ? null : levelList;
    }

}
