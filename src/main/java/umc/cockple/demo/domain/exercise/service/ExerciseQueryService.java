package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
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
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
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

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final GuestRepository guestRepository;
    private final PartyRepository partyRepository;
    private final ExerciseBookmarkRepository exerciseBookmarkRepository;

    private final ExerciseConverter exerciseConverter;

    public ExerciseDetailDTO.Response getExerciseDetail(Long exerciseId, Long memberId) {

        log.info("운동 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

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

        Exercise exercise = findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        List<Guest> myGuests = findGuestsByExerciseIdAndInviterId(exerciseId, memberId);

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

        Party party = findPartyWithLevelsOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);
        validateGetPartyExerciseCalender(startDate, endDate, party);

        Boolean isMember = isPartyMember(party, member);
        DateRange dateRange = calculateDateRange(startDate, endDate);

        List<Exercise> exercises = findExercisesByPartyIdAndDateRange(partyId, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 운동이 없어 빈 응답 반환 - partyId: {}, 기간: {} ~ {}",
                    partyId, dateRange.start(), dateRange.end());

            return exerciseConverter.toEmptyPartyCalendarResponse(
                    dateRange.start(), dateRange.end(), isMember, party);
        }

        Map<Long, Integer> participantCounts = getParticipantCountsMap(
                partyId, dateRange.start(), dateRange.end());

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = getExerciseBookmarkStatus(memberId, exerciseIds);

        log.info("모임 운동 캘린더 조회 완료 - partyId: {}, 조회된 운동 수: {}", partyId, exercises.size());

        return exerciseConverter.toPartyCalendarResponse(
                exercises, dateRange.start(), dateRange.end(), isMember, party, participantCounts, bookmarkStatus);
    }

    public MyExerciseCalendarDTO.Response getMyExerciseCalendar(Long memberId, LocalDate startDate, LocalDate endDate) {

        log.info("내 운동 캘린더 조회 시작 - memberId = {}, startDate = {}, endDate = {}",
                memberId, startDate, endDate);

        Member member = findMemberOrThrow(memberId);
        validateGetMyExerciseCalendar(startDate, endDate);

        DateRange dateRange = calculateDateRange(startDate, endDate);

        List<Exercise> exercises = findExercisesByMemberIdAndDateRange(memberId, dateRange.start(), dateRange.end());

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

        Member member = findMemberOrThrow(memberId);

        List<Long> myPartyIds = findPartyIdsByMemberId(memberId);

        if (myPartyIds.isEmpty()) {
            log.info("내가 속한 모임이 없음 - memberId = {}", memberId);
            return exerciseConverter.toEmptyMyPartyExerciseResponse();
        }

        Pageable pageable = PageRequest.of(0, 6);
        List<Exercise> recentExercises = findRecentExercisesByPartyIds(myPartyIds, pageable);

        log.info("내 모임 운동 조회 종료 - 조회된 운동 수 = {}", recentExercises.size());

        return exerciseConverter.toMyPartyExerciseDTO(recentExercises);
    }

    public MyPartyExerciseCalendarDTO.Response getMyPartyExerciseCalendar(
            Long memberId, MyPartyExerciseOrderType orderType, LocalDate startDate, LocalDate endDate) {

        log.info("내 모임 운동 캘린더 조회 시작 - memberId = {}, orderType = {}, 기간 = {}~{}", memberId, orderType, startDate, endDate);

        Member member = findMemberOrThrow(memberId);
        List<Long> myPartyIds = findPartyIdsByMemberId(memberId);

        DateRange dateRange = calculateDateRange(startDate, endDate);

        if (myPartyIds.isEmpty()) {
            log.info("내가 속한 모임이 없음 - memberId = {}", memberId);
            return exerciseConverter.toEmptyMyPartyCalendarResponse(dateRange.start(), dateRange.end());
        }

        List<Exercise> exercises = findByPartyIdsAndDateRange(myPartyIds, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 내 모임의 운동이 없어 빈 응답 반환 - memberId: {}, 기간: {} ~ {}",
                    memberId, dateRange.start(), dateRange.end());
            return exerciseConverter.toEmptyMyPartyCalendarResponse(dateRange.start(), dateRange.end());
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = getExerciseBookmarkStatus(memberId, exerciseIds);

        Map<Long, Integer> participantCounts = getParticipantCountsMap(exerciseIds, dateRange.start(), dateRange.end());

        log.info("내 운동 캘린더 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyPartyCalendarResponse(
                exercises, dateRange.start(), dateRange.end(), bookmarkStatus, orderType, participantCounts);
    }

    public ExerciseRecommendationDTO.Response getRecommendedExercises(Long memberId) {

        log.info("운동 추천 조회 시작 - memberId: {}", memberId);

        Member member = findMemberWithAddressesOrThrow(memberId);
        MemberAddr mainAddr = findMainAddrOrThrow(member);

        List<Exercise> candidateExercises = findRecommendedExercises(
                memberId, member.getGender(), member.getLevel(), member.getAge());

        List<ExerciseWithDistance> finalExercisesWithDistance = getFinalSortedExercises(candidateExercises, mainAddr);
        List<Exercise> finalExercises = extractExercises(finalExercisesWithDistance);

        List<Long> exerciseIds = getExerciseIds(finalExercises);
        Map<Long, Boolean> bookmarkStatus = getExerciseBookmarkStatus(memberId, exerciseIds);

        log.info("운동 추천 조회 종료 - memberId: {}, 결과 : {}", memberId, exerciseIds.size());

        return exerciseConverter.toExerciseRecommendationResponse(finalExercises, bookmarkStatus);
    }
    
    public MyExerciseListDTO.Response getMyExercises(
            Long memberId, MyExerciseFilterType filterType, MyExerciseOrderType orderType, Pageable pageable) {

        log.info("내 참여 운동 조회 시작 - memberId: {}, filterType: {}, orderType: {}",
                memberId, filterType, orderType);

        Member member = findMemberOrThrow(memberId);

        Pageable sortedPageable = createSortedPageable(pageable, filterType, orderType);

        Slice<Exercise> exerciseSlice = findExercisesByFilterType(memberId, filterType, sortedPageable);

        if (exerciseSlice.isEmpty()) {
            log.info("조회된 운동이 없음 - memberId: {}, filterType: {}", memberId, filterType);
            return exerciseConverter.toEmptyMyExerciseList();
        }

        List<Exercise> exercises = exerciseSlice.getContent();
        List<Long> exerciseIds = exercises.stream().map(Exercise::getId).toList();

        Map<Long, Integer> participantCountMap = getParticipantCountsMap(exerciseIds);
        Map<Long, Boolean> bookmarkStatus = getExerciseBookmarkStatus(memberId, exerciseIds);
        Map<Long, Boolean> isCompletedMap = getExerciseCompletionStatus(exercises);

        log.info("내 참여 운동 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyExerciseListResponse(exerciseSlice, participantCountMap, bookmarkStatus, isCompletedMap);
    }

    public ExerciseBuildingDetailDTO.Response getBuildingExerciseDetails(
            String buildingName, String streetAddr, LocalDate date, Long memberId) {

        log.info("건물 운동 상세 조회 시작 - 건물: {}, 주소: {}, 날짜: {}", buildingName, streetAddr, date);

        Member member = findMemberOrThrow(memberId);
        List<Exercise> exercises = findExercisesByBuildingAndDate(buildingName, streetAddr, date);

        if (exercises.isEmpty()) {
            log.info("건물에 운동이 존재하지 않습니다. - 건물: {}, 주소: {}, 날짜: {}", buildingName, streetAddr, date);
            return exerciseConverter.toEmptyBuildingDetailResponse(buildingName, date);
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = getExerciseBookmarkStatus(memberId, exerciseIds);

        log.info("건물 운동 상세 조회 종료 - 건물: {}, 주소: {}, 날짜: {}, 결과: {}", buildingName, streetAddr, date, exerciseIds.size());

        return exerciseConverter.toBuildingDetailResponse(exercises, buildingName, bookmarkStatus, date);
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
        return memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), member.getId(), Role.party_MANAGER);
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
        List<MemberExercise> memberExercises = findMemberExercisesWithMemberAndProfile(exerciseId);
        List<ExerciseDetailDTO.ParticipantInfo> memberParticipants = buildMemberParticipantInfos(memberExercises, party);

        List<Guest> guests = findGuests(exerciseId);
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

        int size = allParticipants.size();
        for (int i = 0; i < Math.min(size, maxCapacity); i++) {
            ExerciseDetailDTO.ParticipantInfo participant = allParticipants.get(i);
            if ("GUEST".equals(participant.participantType())) {
                guestNumberMap.put(participant.participantId(),
                        ExerciseMyGuestListDTO.GuestGroups.participant(i + 1));
            }
        }

        if (size > maxCapacity) {
            int waitingNumber = 1;
            for (int i = maxCapacity; i < size; i++) {
                ExerciseDetailDTO.ParticipantInfo participant = allParticipants.get(i);
                if ("GUEST".equals(participant.participantType())) {
                    guestNumberMap.put(participant.participantId(),
                            ExerciseMyGuestListDTO.GuestGroups.waiting(waitingNumber));
                    waitingNumber++;
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
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    private DateRange calculateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return new DateRange(startDate, endDate);
        }

        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate defaultStart = thisWeekMonday.minusWeeks(1);
        LocalDate defaultEnd = thisWeekMonday.plusWeeks(3).plusDays(6);

        return new DateRange(defaultStart, defaultEnd);
    }

    private List<ExerciseWithDistance> getFinalSortedExercises(List<Exercise> candidateExercises, MemberAddr mainAddr) {
        return candidateExercises.stream()
                .map(exercise -> {
                    float distance = calculateDistance(
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

    // 하버사인 공식을 이용한 거리 계산
    private float calculateDistance(Float latitude, Float longitude, Float latitude1, Float longitude1) {
        final double R = 6371; // 지구 반지름 (km)

        double latDistance = Math.toRadians(latitude1 - latitude);
        double lonDistance = Math.toRadians(longitude1 - longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(latitude1))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (R * c);
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

    // ========== 세부 비즈니스 메서드 ==========

    private List<ParticipantInfo> buildMemberParticipantInfos(List<MemberExercise> memberExercises, Party party) {
        if (memberExercises.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = memberExercises.stream()
                .map(me -> me.getMember().getId())
                .toList();

        Map<Long, Role> partyMemberRoles = memberPartyRepository
                .findMemberRolesByPartyAndMembers(party.getId(), memberIds)
                .stream()
                .collect(Collectors.toMap(
                        mp -> mp.getMember().getId(),
                        MemberParty::getRole
                ));

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

        Map<Long, String> inviterNames = memberRepository.findMemberNamesByIds(inviterIds);

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
                .imgUrl(original.imgUrl())
                .name(original.name())
                .gender(original.gender())
                .level(original.level())
                .participantType(original.participantType())
                .partyPosition(original.partyPosition())
                .inviterName(original.inviterName())
                .joinedAt(original.joinedAt())
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

    private MemberAddr findMainAddrOrThrow(Member member) {
        return member.getAddresses().stream()
                .filter(MemberAddr::getIsMain)
                .findFirst()
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MAIN_ADDRESS_NULL));
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

    // ========== 조회 메서드 ==========

    private Exercise findExerciseWithBasicInfoOrThrow(Long exerciseId) {
        return exerciseRepository.findExerciseWithBasicInfo(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private List<Exercise> findExercisesByPartyIdAndDateRange(Long partyId, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByPartyIdAndDateRange(partyId, startDate, endDate);
    }

    private List<Exercise> findExercisesByMemberIdAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByMemberIdAndDateRange(memberId, startDate, endDate);
    }

    private List<Exercise> findRecentExercisesByPartyIds(List<Long> myPartyIds, Pageable pageable) {
        return exerciseRepository.findRecentExercisesByPartyIds(myPartyIds, pageable);
    }

    private List<Exercise> findRecommendedExercises(Long memberId, Gender gender, Level level, int age) {
        return exerciseRepository.findExercisesByMemberIdAndLevelAndAge(memberId, gender, level, age);
    }

    private List<Exercise> findByPartyIdsAndDateRange(
            List<Long> myPartyIds, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByPartyIdsAndDateRange(myPartyIds, startDate, endDate);
    }
  
    private Slice<Exercise> findExercisesByFilterType(Long memberId, MyExerciseFilterType filterType, Pageable pageable) {
        return switch (filterType) {
            case ALL -> exerciseRepository.findMyExercisesWithPaging(memberId, pageable);
            case UPCOMING -> exerciseRepository.findMyUpcomingExercisesWithPaging(memberId, pageable);
            case COMPLETED -> exerciseRepository.findMyCompletedExercisesWithPaging(memberId, pageable);
    };

    private List<Exercise> findExercisesByBuildingAndDate(String buildingName, String streetAddr, LocalDate date) {
        return exerciseRepository
                .findExercisesByBuildingAndDate(buildingName, streetAddr, date);
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private Member findMemberWithAddressesOrThrow(Long memberId) {
        return memberRepository.findMemberWithAddresses(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private List<MemberExercise> findMemberExercisesWithMemberAndProfile(Long exerciseId) {
        return memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exerciseId, MemberStatus.ACTIVE);
    }

    private List<Guest> findGuests(Long exerciseId) {
        return guestRepository.findByExerciseId(exerciseId);
    }

    private List<Guest> findGuestsByExerciseIdAndInviterId(Long exerciseId, Long memberId) {
        return guestRepository.findByExerciseIdAndInviterId(exerciseId, memberId);
    }

    private Party findPartyWithLevelsOrThrow(Long partyId) {
        return partyRepository.findByIdWithLevels(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }

    private List<Long> findPartyIdsByMemberId(Long memberId) {
        return memberPartyRepository.findPartyIdsByMemberId(memberId);
    }

    private Map<Long, Integer> getParticipantCountsMap(Long partyId, LocalDate start, LocalDate end) {
        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCounts(
                partyId, start, end);

        return countResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));
    }

    private Map<Long, Integer> getParticipantCountsMap(List<Long> exerciseIds, LocalDate start, LocalDate end) {
        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                exerciseIds, start, end);

        return countResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));
    }

    private Map<Long, Integer> getParticipantCountsMap(List<Long> exerciseIds) {
        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                exerciseIds);

        return countResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));
    }

    private Map<Long, Boolean> getExerciseBookmarkStatus(Long memberId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> bookmarkedExerciseIds = exerciseBookmarkRepository
                .findAllExerciseIdsByMemberIdAndExerciseIds(memberId, exerciseIds);

        return exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        bookmarkedExerciseIds::contains
                ));
    }

    private record ParticipantGroups(
            List<ExerciseDetailDTO.ParticipantInfo> participants,
            List<ExerciseDetailDTO.ParticipantInfo> waiting
    ) {
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    private record ExerciseWithDistance(Exercise exercise, double distance) {
    }
}
