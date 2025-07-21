package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO.ParticipantInfo;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalenderDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.MemberStatus;
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

        return exerciseConverter.toDetailResponseDTO(isManager, exerciseInfo, participantGroup, waitingGroup);
    }

    public ExerciseMyGuestListDTO.Response getMyInvitedGuests(Long exerciseId, Long memberId) {

        log.info("내가 초대한 게스트 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        List<Guest> myGuests = findGuestsByExerciseIdAndInviterId(exerciseId, memberId);

        List<ExerciseDetailDTO.ParticipantInfo> allParticipants = getAllSortedParticipants(exerciseId, exercise.getParty());
        Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap = createGuestNumberMap(allParticipants, exercise.getMaxCapacity());

        String inviterName = member.getMemberName();
        List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList = buildGuestInfoList(myGuests, guestNumberMap, inviterName);

        ExerciseMyGuestListDTO.GuestStatistics statistics = calculateGuestStatistics(guestInfoList);

        log.info("내가 초대한 게스트 조회 완료 - exerciseId: {}", exerciseId);

        return exerciseConverter.toMyGuestListResponse(statistics, guestInfoList);
    }

    public PartyExerciseCalenderDTO.Response getPartyExerciseCalender(Long partyId, Long memberId, LocalDate startDate, LocalDate endDate) {

        log.info("모임 운동 캘린더 조회 시작 - partyId = {}, memberId = {}, startDate = {}, endDate = {}",
                partyId, memberId, startDate, endDate);

        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);
        validateGetPartyExerciseCalender(startDate, endDate);

        Boolean isMember = isPartyMember(party, member);
        DateRange dateRange = calculateDateRange(startDate, endDate);

        List<Exercise> exercises = findExercisesByPartyIdAndDateRange(partyId, dateRange.start(), dateRange.end());

        log.info("모임 운동 캘린더 조회 완료 - partyId: {}, 조회된 운동 수: {}", partyId, exercises.size());

        return exerciseConverter.toCalenderResponse(exercises, dateRange.start(), dateRange.end(), isMember, party.getPartyName());
    }

    // ========== 검증 메서드들 ==========

    private void validateGetPartyExerciseCalender(LocalDate startDate, LocalDate endDate) {
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
                        return exerciseConverter.toParticipantInfo(me, partyMemberRoles);
                    } else {
                        return exerciseConverter.toExeternalParticipantInfo(me);
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
                    return exerciseConverter.toParticipantInfo(guest, inviterName);
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

    // ========== 조회 메서드 ==========

    private Exercise findExerciseWithBasicInfoOrThrow(Long exerciseId) {
        return exerciseRepository.findExerciseWithBasicInfo(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private List<Exercise> findExercisesByPartyIdAndDateRange(Long partyId, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByPartyIdAndDateRange(
                partyId, startDate, endDate);
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
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

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }

    private record ParticipantGroups(
            List<ExerciseDetailDTO.ParticipantInfo> participants,
            List<ExerciseDetailDTO.ParticipantInfo> waiting
    ) {
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}
