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
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExerciseConverter {

    // ========== Command 변환 메서드들 ==========
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

    public ExerciseGuestInviteDTO.Command toGuestInviteCommand(ExerciseGuestInviteDTO.Request request, Long inviterId) {
        return ExerciseGuestInviteDTO.Command.builder()
                .guestName(request.guestName())
                .gender(request.toParsedGender())
                .level(request.toParsedLevel())
                .inviterId(inviterId)
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

    // ========== Response 변환 메서드들 ==========
    public ExerciseCreateDTO.Response toCreateResponse(Exercise exercise) {
        return ExerciseCreateDTO.Response.builder()
                .exerciseId(exercise.getId())
                .createdAt(exercise.getCreatedAt())
                .build();
    }

    public ExerciseJoinDTO.Response toJoinResponse(MemberExercise memberExercise, Exercise exercise) {
        return ExerciseJoinDTO.Response.builder()
                .participantId(memberExercise.getId())
                .joinedAt(memberExercise.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseGuestInviteDTO.Response toGuestInviteResponse(Guest guest, Exercise exercise) {
        return ExerciseGuestInviteDTO.Response.builder()
                .guestId(guest.getId())
                .invitedAt(guest.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(Exercise exercise, Member member) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(member.getMemberName())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(Exercise exercise, Guest guest) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(guest.getGuestName())
                .currentParticipants(exercise.getNowCapacity())
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

    public PartyExerciseCalendarDTO.Response toEmptyPartyCalendarResponse(
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party) {

        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .isMember(isMember)
                .partyName(party.getPartyName())
                .weeks(Collections.emptyList())
                .build();
    }

    public PartyExerciseCalendarDTO.Response toPartyCalendarResponse(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus) {

        PartyLevelCache levelCache = createPartyLevelCache(party);

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = groupPartyExerciseByWeek(exercises, levelCache, participantCounts, bookmarkStatus, start, end);

        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .isMember(isMember)
                .partyName(party.getPartyName())
                .weeks(weeks)
                .build();
    }

    public MyExerciseCalendarDTO.Response toEmptyMyCalendarResponse(LocalDate start, LocalDate end) {
        return MyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(Collections.emptyList())
                .build();
    }

    public MyExerciseCalendarDTO.Response toMyCalendarResponse(List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarDTO.WeeklyExercises> weeks = groupMyExerciseByWeek(exercises, start, end);

        return MyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    public MyPartyExerciseDTO.Response toEmptyMyPartyExerciseResponse() {
        return MyPartyExerciseDTO.Response.builder()
                .totalExercises(0)
                .exercises(List.of())
                .build();
    }

    public MyPartyExerciseDTO.Response toMyPartyExerciseDTO(List<Exercise> recentExercises) {

        List<MyPartyExerciseDTO.Exercises> exercises = recentExercises.stream()
                .map(this::toPartyExerciseItem)
                .toList();

        return MyPartyExerciseDTO.Response.builder()
                .totalExercises(recentExercises.size())
                .exercises(exercises)
                .build();
    }

    public MyPartyExerciseCalendarDTO.Response toEmptyMyPartyCalendarResponse(LocalDate start, LocalDate end) {
        return MyPartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(Collections.emptyList())
                .build();
    }

    public MyPartyExerciseCalendarDTO.Response toMyPartyCalendarResponse(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        List<MyPartyExerciseCalendarDTO.WeeklyExercises> weeks =
                groupMyPartyExerciseByWeek(exercises, start, end, bookmarkStatus, orderType, participantCounts);

        return MyPartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    public ExerciseRecommendationDTO.Response toExerciseRecommendationResponse(
            List<Exercise> finalExercises, Map<Long, Boolean> bookmarkStatus) {

        List<ExerciseRecommendationDTO.ExerciseItem> exercises = finalExercises.stream()
                .map(exercise -> toExerciseRecommendationItem(exercise, bookmarkStatus))
                .toList();

        return ExerciseRecommendationDTO.Response.builder()
                .totalExercises(finalExercises.size())
                .exercises(exercises)
                .build();
    }

    public ExerciseBuildingDetailDTO.Response toEmptyBuildingDetailResponse(String buildingName, LocalDate date) {
        return ExerciseBuildingDetailDTO.Response.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .buildingName(buildingName)
                .exercises(List.of())
                .build();
    }

    public ExerciseBuildingDetailDTO.Response toBuildingDetailResponse(
            List<Exercise> exercises, String buildingName, Map<Long, Boolean> bookmarkStatus, LocalDate date) {

        List<ExerciseBuildingDetailDTO.ExerciseItem> finalExercises = exercises.stream()
                .map(exercise -> toBuildingDetailItem(exercise, bookmarkStatus))
                .toList();

        return ExerciseBuildingDetailDTO.Response.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .buildingName(buildingName)
                .exercises(finalExercises)
                .build();
    }

    // ========== 내부 객체 변환 메서드들 ==========
    public ExerciseDetailDTO.ParticipantInfo toParticipantInfoFromMember(MemberExercise memberParticipant, Map<Long, Role> memberRoles) {
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

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfoFromExternalMember(MemberExercise memberParticipant) {
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

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfoFromGuest(Guest guest, String inviterName) {

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

    // ========== private 메서드들 ==========
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

    // 주별 그룹화 메서드
    private List<PartyExerciseCalendarDTO.WeeklyExercises> groupPartyExerciseByWeek(
            List<Exercise> exercises,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            LocalDate start,
            LocalDate end) {

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<PartyExerciseCalendarDTO.DailyExercises> dailyExercisesList =
                    groupPartyExerciseByDate(weekExercises, weekStart, weekEnd, levelCache, participantCounts, bookmarkStatus);

            weeks.add(this.createPartyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<MyExerciseCalendarDTO.WeeklyExercises> groupMyExerciseByWeek(
            List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyExerciseCalendarDTO.DailyExercises> dailyExercisesList =
                    groupMyExerciseByDate(weekExercises, weekStart, weekEnd);

            weeks.add(createMyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<MyPartyExerciseCalendarDTO.WeeklyExercises> groupMyPartyExerciseByWeek(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        List<MyPartyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyPartyExerciseCalendarDTO.DailyExercises> dailyExercisesList =
                    groupMyPartyExercisesByDate(weekExercises, weekStart, weekEnd, bookmarkStatus, orderType, participantCounts);

            weeks.add(createMyPartyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    // 날짜별 그룹화 메서드
    private List<PartyExerciseCalendarDTO.DailyExercises> groupPartyExerciseByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<PartyExerciseCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(exercise -> toPartyCalendarItem(exercise, levelCache, participantCounts, bookmarkStatus))
                    .toList();

            dailyExercisesList.add(createPartyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private List<MyExerciseCalendarDTO.DailyExercises> groupMyExerciseByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<MyExerciseCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(this::toMyCalendarItem)
                    .toList();

            dailyExercisesList.add(createMyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private List<MyPartyExerciseCalendarDTO.DailyExercises> groupMyPartyExercisesByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<MyPartyExerciseCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            if (Objects.requireNonNull(orderType) == MyPartyExerciseOrderType.LATEST) {
                dayExercises.sort(Comparator.comparing(Exercise::getStartTime));
            } else if (orderType == MyPartyExerciseOrderType.POPULARITY) {
                dayExercises.sort(Comparator.comparingInt((Exercise e) -> participantCounts.getOrDefault(e.getId(), 0)).reversed());
            }

            List<MyPartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(exercise -> toMyPartyCalendarItem(exercise, bookmarkStatus, participantCounts))
                    .toList();

            dailyExercisesList.add(createMyPartyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    // 주별 운동 변환
    private PartyExerciseCalendarDTO.WeeklyExercises createPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<PartyExerciseCalendarDTO.DailyExercises> days) {

        return PartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private MyExerciseCalendarDTO.WeeklyExercises createMyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyExerciseCalendarDTO.DailyExercises> days) {

        return MyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private MyPartyExerciseCalendarDTO.WeeklyExercises createMyPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyPartyExerciseCalendarDTO.DailyExercises> days) {

        return MyPartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    // 날짜별 운동 변환
    private PartyExerciseCalendarDTO.DailyExercises createPartyDailyExercises(
            LocalDate date,
            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return PartyExerciseCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private MyExerciseCalendarDTO.DailyExercises createMyDailyExercises(
            LocalDate date,
            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return MyExerciseCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private MyPartyExerciseCalendarDTO.DailyExercises createMyPartyDailyExercises(
            LocalDate date,
            List<MyPartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return MyPartyExerciseCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    // 운동 아이템 변환 메서드
    private PartyExerciseCalendarDTO.ExerciseCalendarItem toPartyCalendarItem(
            Exercise exercise,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus) {

        Integer currentParticipants = participantCounts.getOrDefault(exercise.getId(), 0);

        return PartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .femaleLevel(levelCache.femaleLevel())
                .maleLevel(levelCache.maleLevel())
                .currentParticipants(currentParticipants)
                .maxCapacity(exercise.getMaxCapacity())
                .build();
    }

    private MyExerciseCalendarDTO.ExerciseCalendarItem toMyCalendarItem(Exercise exercise) {

        Party party = exercise.getParty();

        return MyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .build();
    }

    private MyPartyExerciseDTO.Exercises toPartyExerciseItem(Exercise exercise) {
        Party party = exercise.getParty();

        return MyPartyExerciseDTO.Exercises.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .profileImageUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .build();
    }

    private MyPartyExerciseCalendarDTO.ExerciseCalendarItem toMyPartyCalendarItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus, Map<Long, Integer> participantCounts) {

        Party party = exercise.getParty();

        return MyPartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .nowCapacity(participantCounts.getOrDefault(exercise.getId(), 0))
                .build();
    }

    private ExerciseRecommendationDTO.ExerciseItem toExerciseRecommendationItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseRecommendationDTO.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .imageUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private ExerciseBuildingDetailDTO.ExerciseItem toBuildingDetailItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseBuildingDetailDTO.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .partyImgUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .build();
    }

    private record PartyLevelCache(
            List<String> femaleLevel,
            List<String> maleLevel
    ) {
    }

}
