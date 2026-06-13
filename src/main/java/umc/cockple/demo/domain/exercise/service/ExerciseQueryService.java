package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO.ParticipantInfo;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.service.support.ExerciseBookmarkReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseDistanceCalculator;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.GuestReader;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.service.support.PartyLookupService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseQueryService {

    private final ExerciseReader exerciseReader;
    private final GuestReader guestReader;
    private final ExerciseParticipantReader exerciseParticipantReader;
    private final ExerciseBookmarkReader exerciseBookmarkReader;
    private final ExerciseDistanceCalculator exerciseDistanceCalculator;
    private final MemberLookupService memberLookupService;
    private final PartyLookupService partyLookupService;

    private final ExerciseConverter exerciseConverter;

    public ExerciseDetailDTO.Response getExerciseDetail(Long exerciseId, Long memberId) {

        log.info("운동 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        Party party = exercise.getParty();
        boolean isManager = checkManagerPermission(party, member);

        ExerciseDetailDTO.ExerciseInfo exerciseInfo = createExerciseInfo(exercise);

        List<ExerciseDetailDTO.ParticipantInfo> allParticipants = getAllSortedParticipants(exerciseId, party);
        ParticipantGroups groups = splitParticipants(allParticipants, exercise.getMaxCapacity());

        ExerciseDetailDTO.ParticipantGroup participantGroup = createParticipantGroup(groups.participants(), exercise.getMaxCapacity());
        ExerciseDetailDTO.WaitingGroup waitingGroup = createWaitingGroup(groups.waiting());

        return exerciseConverter.toDetailResponse(isManager, exerciseInfo, participantGroup, waitingGroup);
    }

    public ExerciseMyGuestListDTO.Response getMyInvitedGuests(Long exerciseId, Long memberId) {

        log.info("내가 초대한 게스트 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        List<Guest> myGuests = guestReader.findByExerciseIdAndInviterId(exerciseId, memberId);

        if (myGuests.isEmpty()) {
            log.info("초대한 게스트가 없어 빈 응답 반환 - exerciseId: {}, memberId: {}", exerciseId, memberId);
            return exerciseConverter.toEmptyGuestListResponse();
        }

        List<ExerciseDetailDTO.ParticipantInfo> allParticipants = getAllSortedParticipants(exerciseId, exercise.getParty());
        Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap = createGuestNumberMap(allParticipants, exercise.getMaxCapacity());

        String inviterName = member.getMemberName();
        List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList = buildGuestInfoList(myGuests, guestNumberMap, inviterName);

        ExerciseMyGuestListDTO.GuestStatistics statistics = calculateGuestStatistics(guestInfoList);

        log.info("내가 초대한 게스트 조회 완료 - exerciseId: {}", exerciseId);

        return exerciseConverter.toMyGuestListResponse(statistics, guestInfoList);
    }

    public PartyExerciseCalendarDTO.Response getPartyExerciseCalendar(Long partyId, Long memberId, LocalDate startDate, LocalDate endDate) {

        log.info("모임 운동 캘린더 조회 시작 - partyId = {}, memberId = {}, startDate = {}, endDate = {}",
                partyId, memberId, startDate, endDate);

        Party party = partyLookupService.findByIdWithLevelsOrThrow(partyId);
        Member member = memberLookupService.findByIdOrThrow(memberId);
        validateGetPartyExerciseCalender(startDate, endDate, party);

        Boolean isMember = isPartyMember(party, member);
        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        List<Exercise> exercises = exerciseReader.findByPartyIdAndDateRange(partyId, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 운동이 없어 빈 응답 반환 - partyId: {}, 기간: {} ~ {}",
                    partyId, dateRange.start(), dateRange.end());

            return exerciseConverter.toEmptyPartyCalendarResponse(
                    dateRange.start(), dateRange.end(), isMember, party);
        }

        Map<Long, Integer> participantCounts = exerciseParticipantReader.getParticipantCountsMap(
                partyId, dateRange.start(), dateRange.end());

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkReader.getBookmarkStatus(memberId, exerciseIds);
        Map<Long, Boolean> participatingStatus = exerciseParticipantReader.getParticipatingStatus(memberId, exerciseIds);

        log.info("모임 운동 캘린더 조회 완료 - partyId: {}, 조회된 운동 수: {}", partyId, exercises.size());

        return exerciseConverter.toPartyCalendarResponse(
                exercises, dateRange.start(), dateRange.end(), isMember, party, participantCounts, bookmarkStatus, participatingStatus);
    }

    public MyExerciseCalendarDTO.Response getMyExerciseCalendar(Long memberId, LocalDate startDate, LocalDate endDate) {

        log.info("내 운동 캘린더 조회 시작 - memberId = {}, startDate = {}, endDate = {}",
                memberId, startDate, endDate);

        Member member = memberLookupService.findByIdOrThrow(memberId);
        validateGetMyExerciseCalendar(startDate, endDate);

        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        List<Exercise> exercises = exerciseReader.findByMemberIdAndDateRange(memberId, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 참여한 운동이 없어 빈 응답 반환 - memberId: {}, 기간: {} ~ {}",
                    memberId, dateRange.start(), dateRange.end());
            return exerciseConverter.toEmptyMyCalendarResponse(dateRange.start(), dateRange.end());
        }

        log.info("내 운동 캘린더 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyCalendarResponse(exercises, dateRange.start(), dateRange.end());
    }

    public MyPartyExerciseDTO.Response getMyPartyExercise(Long memberId) {

        log.info("내 모임 운동 조회 시작 - memberId = {}", memberId);

        Member member = memberLookupService.findByIdOrThrow(memberId);

        List<Long> myPartyIds = exerciseParticipantReader.findPartyIdsByMemberId(memberId);

        if (myPartyIds.isEmpty()) {
            log.info("내가 속한 모임이 없음 - memberId = {}", memberId);
            return exerciseConverter.toEmptyMyPartyExerciseResponse();
        }

        Pageable pageable = PageRequest.of(0, 6);
        List<Exercise> recentExercises = exerciseReader.findRecentByPartyIds(myPartyIds, pageable);

        log.info("내 모임 운동 조회 종료 - 조회된 운동 수 = {}", recentExercises.size());

        return exerciseConverter.toMyPartyExerciseDTO(recentExercises);
    }

    public MyPartyExerciseCalendarDTO.Response getMyPartyExerciseCalendar(
            Long memberId, MyPartyExerciseOrderType orderType, LocalDate startDate, LocalDate endDate) {

        log.info("내 모임 운동 캘린더 조회 시작 - memberId = {}, orderType = {}, 기간 = {}~{}", memberId, orderType, startDate, endDate);

        Member member = memberLookupService.findByIdOrThrow(memberId);
        List<Long> myPartyIds = exerciseParticipantReader.findPartyIdsByMemberId(memberId);

        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        if (myPartyIds.isEmpty()) {
            log.info("내가 속한 모임이 없음 - memberId = {}", memberId);
            return exerciseConverter.toEmptyMyPartyCalendarResponse(dateRange.start(), dateRange.end());
        }

        List<Exercise> exercises = exerciseReader.findByPartyIdsAndDateRange(myPartyIds, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 내 모임의 운동이 없어 빈 응답 반환 - memberId: {}, 기간: {} ~ {}",
                    memberId, dateRange.start(), dateRange.end());
            return exerciseConverter.toEmptyMyPartyCalendarResponse(dateRange.start(), dateRange.end());
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkReader.getBookmarkStatus(memberId, exerciseIds);

        Map<Long, Integer> participantCounts = exerciseParticipantReader.getParticipantCountsMap(exerciseIds, dateRange.start(), dateRange.end());

        log.info("내 운동 캘린더 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyPartyCalendarResponse(
                exercises, dateRange.start(), dateRange.end(), bookmarkStatus, orderType, participantCounts);
    }

    public ExerciseRecommendationDTO.Response getRecommendedExercises(Long memberId) {

        log.info("운동 추천 조회 시작 - memberId: {}", memberId);

        Member member = memberLookupService.findWithAddressesOrThrow(memberId);
        MemberAddr mainAddr = memberLookupService.findMainAddressOrThrow(member);

        List<Exercise> candidateExercises = exerciseReader.findRecommendedExercises(
                memberId, member.getGender(), member.getLevel(), member.getBirth().getYear());

        List<ExerciseWithDistance> finalExercisesWithDistance = getFinalSortedExercises(candidateExercises, mainAddr);
        List<Exercise> finalExercises = extractExercises(finalExercisesWithDistance);

        List<Long> exerciseIds = getExerciseIds(finalExercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkReader.getBookmarkStatus(memberId, exerciseIds);

        log.info("운동 추천 조회 종료 - memberId: {}, 결과 : {}", memberId, exerciseIds.size());

        return exerciseConverter.toExerciseRecommendationResponse(finalExercises, bookmarkStatus);
    }

    public MyExerciseListDTO.Response getMyExercises(
            Long memberId, MyExerciseFilterType filterType, MyExerciseOrderType orderType, Pageable pageable) {

        log.info("내 참여 운동 조회 시작 - memberId: {}, filterType: {}, orderType: {}",
                memberId, filterType, orderType);

        Member member = memberLookupService.findByIdOrThrow(memberId);

        Pageable sortedPageable = createSortedPageable(pageable, filterType, orderType);

        Slice<Exercise> exerciseSlice = exerciseReader.findByFilterType(memberId, filterType, sortedPageable);

        if (exerciseSlice.isEmpty()) {
            log.info("조회된 운동이 없음 - memberId: {}, filterType: {}", memberId, filterType);
            return exerciseConverter.toEmptyMyExerciseList();
        }

        List<Exercise> exercises = exerciseSlice.getContent();
        List<Long> exerciseIds = exercises.stream().map(Exercise::getId).toList();

        Map<Long, Integer> participantCountMap = exerciseParticipantReader.getParticipantCountsMap(exerciseIds);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkReader.getBookmarkStatus(memberId, exerciseIds);
        Map<Long, Boolean> isCompletedMap = getExerciseCompletionStatus(exercises);

        log.info("내 참여 운동 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyExerciseListResponse(exerciseSlice, participantCountMap, bookmarkStatus, isCompletedMap);
    }

    public ExerciseBuildingDetailDTO.Response getBuildingExerciseDetails(
            String buildingName, String streetAddr, LocalDate date, Long memberId) {

        log.info("건물 운동 상세 조회 시작 - 건물: {}, 주소: {}, 날짜: {}", buildingName, streetAddr, date);

        Member member = memberLookupService.findByIdOrThrow(memberId);
        List<Exercise> exercises = exerciseReader.findByBuildingAndDate(buildingName, streetAddr, date);

        if (exercises.isEmpty()) {
            log.info("건물에 운동이 존재하지 않습니다. - 건물: {}, 주소: {}, 날짜: {}", buildingName, streetAddr, date);
            return exerciseConverter.toEmptyBuildingDetailResponse(buildingName, date);
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkReader.getBookmarkStatus(memberId, exerciseIds);

        log.info("건물 운동 상세 조회 종료 - 건물: {}, 주소: {}, 날짜: {}, 결과: {}", buildingName, streetAddr, date, exerciseIds.size());

        return exerciseConverter.toBuildingDetailResponse(exercises, buildingName, bookmarkStatus, date);
    }

    public ExerciseMapBuildingsDTO.Response getExerciseMapCalendarSummary(
            ExerciseMapBuildingsDTO.Query query, Long memberId) {

        log.info("월간 운동 캘린더 요약 조회 시작 - 날짜: {}, 중심: ({}, {}), 반경: {}km",
                query.date(), query.latitude(), query.longitude(), query.radiusKm());

        Member member = memberLookupService.findWithAddressesOrThrow(memberId);
        MemberAddr mainAddr = memberLookupService.findMainAddressOrThrow(member);
        ExerciseMapBuildingsDTO.Query searchQuery =
                query.withFallbackLocation(mainAddr.getLatitude(), mainAddr.getLongitude());

        DateRange dateRange = DateRange.calculateMonthlyStartAndEnd(query.date());

        List<Exercise> exercises = exerciseReader.findByMonthAndRadius(dateRange.start(), dateRange.end(), searchQuery);

        Map<LocalDate, List<ExerciseMapBuildingsDTO.BuildingInfo>> dailyBuildings =
                groupExercisesByDateAndBuilding(exercises);

        log.info("월간 운동 캘린더 요약 조회 완료 - 조회된 운동 수: {}", exercises.size());

        return exerciseConverter.toMapCalendarSummaryResponse(
                dateRange.start().getYear(), dateRange.start().getMonthValue(),
                searchQuery.latitude(), searchQuery.longitude(), searchQuery.radiusKm(), dailyBuildings);
    }

    public ExerciseRecommendationCalendarDTO.Response getRecommendedExerciseCalendar(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCockpleRecommend,
            ExerciseRecommendationCalendarDTO.FilterSortType filterSortType) {

        log.info("사용자 추천 운동 캘린더 조회 시작 - memberId: {}, 콕플추천: {}, 필터정렬: {}, 기간: {}~{}"
                , memberId, isCockpleRecommend, filterSortType, startDate, endDate);

        Member member = memberLookupService.findWithAddressesOrThrow(memberId);
        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        List<Exercise> exercises;

        if (isCockpleRecommend) {
            exercises = exerciseReader.findCockpleRecommendedByDateRange(member, dateRange.start(), dateRange.end());
        } else {
            exercises = exerciseReader.findFilteredRecommended(member, dateRange.start(), dateRange.end(), filterSortType);
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkReader.getBookmarkStatus(memberId, exerciseIds);
        Map<Long, Integer> participantCountMap = exerciseParticipantReader.getParticipantCountsMap(exerciseIds);
        MemberAddr mainAddr = memberLookupService.findMainAddressOrThrow(member);

        log.info("사용자 추천 운동 캘린더 조회 완료 - memberId: {}, 결과 수: {}", memberId, exercises.size());

        return exerciseConverter.toRecommendationCalendarResponse(
                exercises, bookmarkStatus, participantCountMap, mainAddr
                , dateRange.start(), dateRange.end(), isCockpleRecommend, filterSortType);
    }

    public ExerciseEditDetailDTO.Response getExerciseForEdit(Long exerciseId, Long memberId) {
        log.info("운동 수정용 상세조회 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);
        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        log.info("운동 수정용 상세조회 완료 - exerciseId: {}", exerciseId);
        return exerciseConverter.toEditDetailResponse(exercise);
    }

    // ========== 검증 메서드들 ==========

    private void validateGetPartyExerciseCalender(LocalDate startDate, LocalDate endDate, Party party) {
        validatePartyIsActive(party);
        validateDateRange(startDate, endDate);
    }

    private void validateGetMyExerciseCalendar(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
    }

    // ========== 세부 검증 메서드들 ==========

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }

        if (startDate == null || endDate == null) {
            throw new ExerciseException(ExerciseErrorCode.INCOMPLETE_DATE_RANGE);
        }

        if (!startDate.isBefore(endDate)) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    // ========== 비즈니스 메서드 ==========

    private boolean checkManagerPermission(Party party, Member member) {
        return exerciseParticipantReader.hasManagerPermission(party, member);
    }

    private ExerciseDetailDTO.ExerciseInfo createExerciseInfo(Exercise exercise) {
        ExerciseAddr addr = exercise.getExerciseAddr();

        return ExerciseDetailDTO.ExerciseInfo.builder()
                .notice(exercise.getNotice())
                .buildingName(addr.getBuildingName())
                .location(addr.getStreetAddr())
                .build();
    }

    private List<ExerciseDetailDTO.ParticipantInfo> getAllSortedParticipants(Long exerciseId, Party party) {
        List<MemberExercise> memberExercises = exerciseParticipantReader.findMemberExercisesWithMemberAndProfile(exerciseId);
        List<ExerciseDetailDTO.ParticipantInfo> memberParticipants = buildMemberParticipantInfos(memberExercises, party);

        List<Guest> guests = guestReader.findByExerciseId(exerciseId);
        List<ExerciseDetailDTO.ParticipantInfo> guestParticipants = buildGuestParticipantInfos(guests);

        List<ExerciseDetailDTO.ParticipantInfo> allParticipants = new ArrayList<>();
        allParticipants.addAll(memberParticipants);
        allParticipants.addAll(guestParticipants);

        allParticipants.sort(Comparator.comparing(ExerciseDetailDTO.ParticipantInfo::joinedAt));

        return allParticipants;
    }

    private ParticipantGroups splitParticipants(
            List<ExerciseDetailDTO.ParticipantInfo> allParticipants,
            int maxCapacity) {

        List<ExerciseDetailDTO.ParticipantInfo> participantList = createParticipantList(allParticipants, maxCapacity);
        List<ExerciseDetailDTO.ParticipantInfo> waitingList = createWaitingList(allParticipants, maxCapacity);

        return new ParticipantGroups(participantList, waitingList);
    }

    private ExerciseDetailDTO.ParticipantGroup createParticipantGroup(
            List<ExerciseDetailDTO.ParticipantInfo> participants,
            int maxCapacity) {

        return ExerciseDetailDTO.ParticipantGroup.builder()
                .currentParticipantCount(participants.size())
                .totalCount(maxCapacity)
                .manCount(countByGender(participants, "MALE"))
                .womenCount(countByGender(participants, "FEMALE"))
                .list(participants)
                .build();
    }

    private ExerciseDetailDTO.WaitingGroup createWaitingGroup(
            List<ExerciseDetailDTO.ParticipantInfo> waiting) {

        return ExerciseDetailDTO.WaitingGroup.builder()
                .currentWaitingCount(waiting.size())
                .manCount(countByGender(waiting, "MALE"))
                .womenCount(countByGender(waiting, "FEMALE"))
                .list(waiting)
                .build();
    }

    private Map<Long, ExerciseMyGuestListDTO.GuestGroups> createGuestNumberMap(List<ParticipantInfo> allParticipants, Integer maxCapacity) {
        Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap = new HashMap<>();

        for (int i = 0; i < allParticipants.size(); i++) {
            ExerciseDetailDTO.ParticipantInfo participant = allParticipants.get(i);

            if ("GUEST".equals(participant.participantType())) {
                if (i < maxCapacity) {
                    guestNumberMap.put(participant.participantId(),
                            ExerciseMyGuestListDTO.GuestGroups.participant(i + 1));
                } else {
                    int waitingNumber = i - maxCapacity + 1;
                    guestNumberMap.put(participant.participantId(),
                            ExerciseMyGuestListDTO.GuestGroups.waiting(waitingNumber));
                }
            }
        }

        return guestNumberMap;
    }

    private List<ExerciseMyGuestListDTO.GuestInfo> buildGuestInfoList(
            List<Guest> myGuests,
            Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap,
            String inviterName) {

        return myGuests.stream()
                .map(guest -> exerciseConverter.toGuestInfo(guest, guestNumberMap, inviterName))
                .toList();
    }

    private ExerciseMyGuestListDTO.GuestStatistics calculateGuestStatistics(List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList) {
        int totalCount = guestInfoList.size();
        int maleCount = (int) guestInfoList.stream()
                .filter(guest -> guest.gender() == Gender.MALE)
                .count();
        int femaleCount = totalCount - maleCount;

        return new ExerciseMyGuestListDTO.GuestStatistics(totalCount, maleCount, femaleCount);
    }

    private boolean isPartyMember(Party party, Member member) {
        return exerciseParticipantReader.isPartyMember(party, member);
    }

    private List<ExerciseWithDistance> getFinalSortedExercises(List<Exercise> candidateExercises, MemberAddr mainAddr) {
        return candidateExercises.stream()
                .map(exercise -> {
                    double distance = calculateDistance(
                            mainAddr.getLatitude(),
                            mainAddr.getLongitude(),
                            exercise.getExerciseAddr().getLatitude(),
                            exercise.getExerciseAddr().getLongitude()
                    );
                    return new ExerciseWithDistance(exercise, distance);
                })
                .sorted(Comparator
                        .comparing(ExerciseWithDistance::distance)
                        .thenComparing(ewd -> ewd.exercise().getDate())
                        .thenComparing(ewd -> ewd.exercise().getStartTime())
                )
                .limit(10)
                .toList();
    }

    private double calculateDistance(double latitude, double longitude, double latitude1, double longitude1) {
        return exerciseDistanceCalculator.calculate(latitude, longitude, latitude1, longitude1);
    }

    private static List<Exercise> extractExercises(List<ExerciseWithDistance> finalExercisesWithDistance) {
        return finalExercisesWithDistance.stream()
                .map(ExerciseWithDistance::exercise)
                .toList();
    }

    private Pageable createSortedPageable(
            Pageable pageable, MyExerciseFilterType filterType, MyExerciseOrderType orderType) {
        Sort sort = createSortByFilterAndOrder(filterType, orderType);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Sort createSortByFilterAndOrder(MyExerciseFilterType filterType, MyExerciseOrderType orderType) {
        return switch (filterType) {
            case ALL -> createSortForAll(orderType);
            case UPCOMING -> createSortForUpcoming(orderType);
            case COMPLETED -> createSortForCompleted(orderType);
        };
    }

    private Map<Long, Boolean> getExerciseCompletionStatus(List<Exercise> exercises) {
        return exercises.stream()
                .collect(Collectors.toMap(
                        Exercise::getId,
                        Exercise::isAlreadyStarted
                ));
    }

    private Map<LocalDate, List<ExerciseMapBuildingsDTO.BuildingInfo>> groupExercisesByDateAndBuilding(List<Exercise> exercises) {
        Map<LocalDate, List<Exercise>> exercisesByDate = exercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        return exercisesByDate.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> createBuildingSummariesForDate(entry.getValue()),
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));
    }

    private List<ExerciseMapBuildingsDTO.BuildingInfo> createBuildingSummariesForDate(List<Exercise> dayExercises) {
        Map<BuildingKey, List<Exercise>> exercisesByBuilding = dayExercises.stream()
                .collect(Collectors.groupingBy(this::createBuildingKey));

        return exercisesByBuilding.keySet().stream()
                .map(entry -> exerciseConverter.toBuildingSummary(
                        entry.name(), entry.address(), entry.latitude(), entry.longitude())
                )
                .toList();
    }

    // ========== 세부 비즈니스 메서드 ==========

    private List<ParticipantInfo> buildMemberParticipantInfos(List<MemberExercise> memberExercises, Party party) {
        if (memberExercises.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = memberExercises.stream()
                .map(me -> me.getMember().getId())
                .toList();

        Map<Long, Role> partyMemberRoles = exerciseParticipantReader
                .findMemberRolesByPartyAndMembers(party.getId(), memberIds);

        return memberExercises.stream()
                .map(me -> {
                    if (partyMemberRoles.containsKey(me.getMember().getId())) {
                        return exerciseConverter.toParticipantInfoFromMember(me, partyMemberRoles);
                    } else {
                        return exerciseConverter.toParticipantInfoFromExternalMember(me);
                    }
                })
                .toList();
    }

    private List<ParticipantInfo> buildGuestParticipantInfos(List<Guest> guests) {
        if (guests.isEmpty()) {
            return List.of();
        }

        Set<Long> inviterIds = guests.stream()
                .map(Guest::getInviterId)
                .collect(Collectors.toSet());

        Map<Long, String> inviterNames = memberLookupService.findNamesByIds(inviterIds);

        return guests.stream()
                .map(guest -> {
                    String inviterName = inviterNames.getOrDefault(guest.getInviterId(), "알 수 없음");
                    return exerciseConverter.toParticipantInfoFromGuest(guest, inviterName);
                })
                .toList();
    }

    private List<ExerciseDetailDTO.ParticipantInfo> createParticipantList(
            List<ExerciseDetailDTO.ParticipantInfo> allParticipants,
            int maxCapacity) {

        List<ExerciseDetailDTO.ParticipantInfo> participantList = new ArrayList<>();
        int endIndex = Math.min(allParticipants.size(), maxCapacity);

        for (int i = 0; i < endIndex; i++) {
            ExerciseDetailDTO.ParticipantInfo original = allParticipants.get(i);
            ExerciseDetailDTO.ParticipantInfo participant = createParticipantWithNumber(original, i + 1);
            participantList.add(participant);
        }

        return participantList;
    }

    private List<ExerciseDetailDTO.ParticipantInfo> createWaitingList(
            List<ExerciseDetailDTO.ParticipantInfo> allParticipants,
            int maxCapacity) {

        List<ExerciseDetailDTO.ParticipantInfo> waitingList = new ArrayList<>();

        if (allParticipants.size() <= maxCapacity) {
            return waitingList;
        }

        for (int i = maxCapacity; i < allParticipants.size(); i++) {
            ExerciseDetailDTO.ParticipantInfo original = allParticipants.get(i);
            int waitingNumber = (i - maxCapacity) + 1;
            ExerciseDetailDTO.ParticipantInfo waiting = createParticipantWithNumber(original, waitingNumber);
            waitingList.add(waiting);
        }

        return waitingList;
    }

    private ExerciseDetailDTO.ParticipantInfo createParticipantWithNumber(
            ExerciseDetailDTO.ParticipantInfo original,
            int number) {

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(original.participantId())
                .participantNumber(number)
                .profileImageUrl(original.profileImageUrl())
                .name(original.name())
                .gender(original.gender())
                .level(original.level())
                .participantType(original.participantType())
                .partyPosition(original.partyPosition())
                .inviterName(original.inviterName())
                .joinedAt(original.joinedAt())
                .isWithdrawn(original.isWithdrawn())
                .build();
    }

    private int countByGender(List<ExerciseDetailDTO.ParticipantInfo> participants, String gender) {
        return (int) participants.stream()
                .filter(p -> gender.equals(p.gender()))
                .count();
    }

    private static List<Long> getExerciseIds(List<Exercise> exercises) {
        return exercises.stream().map(Exercise::getId).toList();
    }

    private Sort createSortForAll(MyExerciseOrderType orderType) {
        return switch (orderType) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("date"),
                    Sort.Order.desc("startTime")
            );
            case OLDEST -> Sort.by(
                    Sort.Order.asc("date"),
                    Sort.Order.asc("startTime")
            );
        };
    }

    private Sort createSortForUpcoming(MyExerciseOrderType orderType) {
        return switch (orderType) {
            case LATEST -> Sort.by(
                    Sort.Order.asc("date"),
                    Sort.Order.asc("startTime")
            );
            case OLDEST -> Sort.by(
                    Sort.Order.desc("date"),
                    Sort.Order.desc("startTime")
            );
        };
    }

    private Sort createSortForCompleted(MyExerciseOrderType orderType) {
        return switch (orderType) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("date"),
                    Sort.Order.desc("startTime")
            );
            case OLDEST -> Sort.by(
                    Sort.Order.asc("date"),
                    Sort.Order.asc("startTime")
            );
        };
    }

    private BuildingKey createBuildingKey(Exercise exercise) {
        var addr = exercise.getExerciseAddr();

        return new BuildingKey(
                addr.getBuildingName(),
                addr.getStreetAddr(),
                addr.getLatitude().doubleValue(),
                addr.getLongitude().doubleValue()
        );
    }

    private record ParticipantGroups(
            List<ExerciseDetailDTO.ParticipantInfo> participants,
            List<ExerciseDetailDTO.ParticipantInfo> waiting
    ) {
    }

    private record DateRange(LocalDate start, LocalDate end) {
        private static DateRange calculateMonthlyStartAndEnd(LocalDate date) {
            LocalDate targetDate = (date != null) ? date : LocalDate.now();

            LocalDate start = targetDate.withDayOfMonth(1);
            int lastDay = targetDate.lengthOfMonth();
            LocalDate end = targetDate.withDayOfMonth(lastDay);

            return new DateRange(start, end);
        }

        private static DateRange calculateDateRange(LocalDate startDate, LocalDate endDate) {
            if (startDate != null && endDate != null) {
                return new DateRange(startDate, endDate);
            }

            LocalDate today = LocalDate.now();
            LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate defaultStart = thisWeekMonday.minusWeeks(1);
            LocalDate defaultEnd = thisWeekMonday.plusWeeks(3).plusDays(6);

            return new DateRange(defaultStart, defaultEnd);
        }
    }

    private record ExerciseWithDistance(Exercise exercise, double distance) {
    }

    private record BuildingKey(
            String name,
            String address,
            Double latitude,
            Double longitude
    ) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            BuildingKey that = (BuildingKey) obj;
            return Objects.equals(name, that.name) &&
                    Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, address);
        }
    }
}
