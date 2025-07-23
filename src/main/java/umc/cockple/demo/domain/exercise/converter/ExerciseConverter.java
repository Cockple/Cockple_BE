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

    public PartyExerciseCalendarDTO.Response toCalendarResponse(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party,
            Map<Long, Integer> participantCounts) {

        PartyLevelCache levelCache = createPartyLevelCache(party);

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = groupExerciseByWeek(exercises, levelCache, participantCounts, start, end);

        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .isMember(isMember)
                .partyName(party.getPartyName())
                .weeks(weeks)
                .build();
    }

    public MyExerciseCalendarDTO.Response toCalendarResponse(List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarDTO.WeeklyExercises> weeks = groupExerciseByWeek(exercises, start, end);

        return MyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    private PartyLevelCache createPartyLevelCache(Party party) {
        List<String> femaleLevel = extractLevelsByGender(party, Gender.FEMALE);
        List<String> maleLevel = extractLevelsByGender(party, Gender.MALE);

        return new PartyLevelCache(femaleLevel, maleLevel);
    }

    private List<String> extractLevelsByGender(Party party, Gender gender) {
        List<String> levelList = party.getLevels().stream()
                .filter(l -> l.getGender() == gender)
                .map(l -> l.getLevel().getKoreanName())
                .toList();

        return levelList.isEmpty() ? null : levelList;
    }

    private List<PartyExerciseCalendarDTO.WeeklyExercises> groupExerciseByWeek(
            List<Exercise> exercises,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            LocalDate start,
            LocalDate end) {

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems =
                    convertToExerciseItems(weekExercises, levelCache, participantCounts);

            weeks.add(this.createPartyWeeklyExercises(weekStart, weekEnd, exerciseItems));
        }

        return weeks;
    }

    private List<MyExerciseCalendarDTO.WeeklyExercises> groupExerciseByWeek(List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = convertToExerciseItems(weekExercises);

            weeks.add(createMyWeeklyExercises(weekStart, weekEnd, exerciseItems));
        }

        return weeks;
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    private List<Exercise> filterExercisesByWeek(List<Exercise> exercises, LocalDate weekStart, LocalDate weekEnd) {
        return exercises.stream()
                .filter(exercise -> {
                    LocalDate exerciseDate = exercise.getDate();
                    return !exerciseDate.isBefore(weekStart) && !exerciseDate.isAfter(weekEnd);
                })
                .toList();
    }

    private List<PartyExerciseCalendarDTO.ExerciseCalendarItem> convertToExerciseItems(
            List<Exercise> exercises,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts) {

        return exercises.stream()
                .map(exercise -> toCalendarItem(exercise, levelCache, participantCounts))
                .toList();
    }

    private List<MyExerciseCalendarDTO.ExerciseCalendarItem> convertToExerciseItems(List<Exercise> exercises) {
        return exercises.stream()
                .map(this::toCalendarItem)
                .toList();
    }

    private PartyExerciseCalendarDTO.WeeklyExercises createPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return PartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .exercises(exerciseItems)
                .build();
    }

    private MyExerciseCalendarDTO.WeeklyExercises createMyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return MyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .exercises(exerciseItems)
                .build();
    }

    private PartyExerciseCalendarDTO.ExerciseCalendarItem toCalendarItem(
            Exercise exercise, PartyLevelCache levelCache, Map<Long, Integer> participantCounts) {

        Integer currentParticipants = participantCounts.getOrDefault(exercise.getId(), 0);

        return PartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .femaleLevel(levelCache.femaleLevel())
                .maleLevel(levelCache.maleLevel())
                .currentParticipants(currentParticipants)
                .maxCapacity(exercise.getMaxCapacity())
                .build();
    }

    private MyExerciseCalendarDTO.ExerciseCalendarItem toCalendarItem(Exercise exercise) {

        Party party = exercise.getParty();

        return MyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .build();
    }

    public MyPartyExerciseDTO.Response toEmptyExerciseResponse() {
        return MyPartyExerciseDTO.Response.builder()
                .totalExercises(0)
                .exercises(List.of())
                .build();
    }

    private record PartyLevelCache(
            List<String> femaleLevel,
            List<String> maleLevel
    ) {
    }

}
