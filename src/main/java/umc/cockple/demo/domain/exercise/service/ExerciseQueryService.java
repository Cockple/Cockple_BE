package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO.ParticipantInfo;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.GuestReader;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.party.domain.Party;
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
    private final MemberLookupService memberLookupService;

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

    public ExerciseEditDetailDTO.Response getExerciseForEdit(Long exerciseId, Long memberId) {
        log.info("운동 수정용 상세조회 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);
        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        log.info("운동 수정용 상세조회 완료 - exerciseId: {}", exerciseId);
        return exerciseConverter.toEditDetailResponse(exercise);
    }

    // ========== 비즈니스 메서드 ==========

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

    private record ParticipantGroups(
            List<ExerciseDetailDTO.ParticipantInfo> participants,
            List<ExerciseDetailDTO.ParticipantInfo> waiting
    ) {
    }



}
