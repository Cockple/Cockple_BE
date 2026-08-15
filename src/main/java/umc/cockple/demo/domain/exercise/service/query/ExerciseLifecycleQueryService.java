package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseEditDetailResult;
import umc.cockple.demo.domain.exercise.service.support.assembler.ExerciseParticipantInfoAssembler;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseLifecycleQueryService {

    private final ExerciseReader exerciseReader;
    private final ExerciseParticipantInfoAssembler participantInfoAssembler;
    private final MemberLookupService memberLookupService;
    private final MemberPartyLookupService memberPartyLookupService;
    private final ExerciseValidator exerciseValidator;

    public ExerciseDetailResult getExerciseDetail(Long exerciseId, Long memberId) {

        log.info("운동 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        Party party = exercise.getParty();
        boolean isManager = checkManagerPermission(party, member);

        ExerciseDetailResult.ExerciseInfo exerciseInfo = createExerciseInfo(exercise);

        List<ExerciseDetailResult.ParticipantInfo> allParticipants =
                participantInfoAssembler.getAllSortedParticipants(exerciseId, party);
        ParticipantGroups groups = splitParticipants(allParticipants, exercise.getMaxCapacity());

        ExerciseDetailResult.ParticipantGroup participantGroup =
                createParticipantGroup(groups.participants(), exercise.getMaxCapacity());
        ExerciseDetailResult.WaitingGroup waitingGroup = createWaitingGroup(groups.waiting());

        return new ExerciseDetailResult(isManager, exerciseInfo, participantGroup, waitingGroup);
    }

    public ExerciseEditDetailResult getExerciseForEdit(Long exerciseId, Long memberId) {
        log.info("운동 수정용 상세조회 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);
        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        exerciseValidator.validateExerciseManagementPermission(exercise, memberId);
        log.info("운동 수정용 상세조회 완료 - exerciseId: {}", exerciseId);

        ExerciseAddr address = exercise.getExerciseAddr();
        return new ExerciseEditDetailResult(
                exercise.getDate(),
                address.getBuildingName(),
                address.getStreetAddr(),
                address.getLatitude(),
                address.getLongitude(),
                exercise.getStartTime(),
                exercise.getEndTime(),
                exercise.getMaxCapacity(),
                exercise.getPartyGuestAccept(),
                exercise.getOutsideGuestAccept(),
                exercise.getNotice()
        );
    }

    // ========== 비즈니스 메서드 ==========

    private boolean checkManagerPermission(Party party, Member member) {
        return memberPartyLookupService.hasRole(party, member, Role.PARTY_MANAGER);
    }

    private ExerciseDetailResult.ExerciseInfo createExerciseInfo(Exercise exercise) {
        ExerciseAddr addr = exercise.getExerciseAddr();

        return new ExerciseDetailResult.ExerciseInfo(
                exercise.getNotice(), addr.getBuildingName(), addr.getStreetAddr());
    }

    private ParticipantGroups splitParticipants(
            List<ExerciseDetailResult.ParticipantInfo> allParticipants,
            int maxCapacity) {

        List<ExerciseDetailResult.ParticipantInfo> participantList =
                createParticipantList(allParticipants, maxCapacity);
        List<ExerciseDetailResult.ParticipantInfo> waitingList =
                createWaitingList(allParticipants, maxCapacity);

        return new ParticipantGroups(participantList, waitingList);
    }

    private ExerciseDetailResult.ParticipantGroup createParticipantGroup(
            List<ExerciseDetailResult.ParticipantInfo> participants,
            int maxCapacity) {

        return new ExerciseDetailResult.ParticipantGroup(
                participants.size(),
                maxCapacity,
                countByGender(participants, "MALE"),
                countByGender(participants, "FEMALE"),
                participants
        );
    }

    private ExerciseDetailResult.WaitingGroup createWaitingGroup(
            List<ExerciseDetailResult.ParticipantInfo> waiting) {

        return new ExerciseDetailResult.WaitingGroup(
                waiting.size(),
                countByGender(waiting, "MALE"),
                countByGender(waiting, "FEMALE"),
                waiting
        );
    }

    private List<ExerciseDetailResult.ParticipantInfo> createParticipantList(
            List<ExerciseDetailResult.ParticipantInfo> allParticipants,
            int maxCapacity) {

        List<ExerciseDetailResult.ParticipantInfo> participantList = new ArrayList<>();
        int endIndex = Math.min(allParticipants.size(), maxCapacity);

        for (int i = 0; i < endIndex; i++) {
            ExerciseDetailResult.ParticipantInfo original = allParticipants.get(i);
            ExerciseDetailResult.ParticipantInfo participant = createParticipantWithNumber(original, i + 1);
            participantList.add(participant);
        }

        return participantList;
    }

    private List<ExerciseDetailResult.ParticipantInfo> createWaitingList(
            List<ExerciseDetailResult.ParticipantInfo> allParticipants,
            int maxCapacity) {

        List<ExerciseDetailResult.ParticipantInfo> waitingList = new ArrayList<>();

        if (allParticipants.size() <= maxCapacity) {
            return waitingList;
        }

        for (int i = maxCapacity; i < allParticipants.size(); i++) {
            ExerciseDetailResult.ParticipantInfo original = allParticipants.get(i);
            int waitingNumber = (i - maxCapacity) + 1;
            ExerciseDetailResult.ParticipantInfo waiting = createParticipantWithNumber(original, waitingNumber);
            waitingList.add(waiting);
        }

        return waitingList;
    }

    private ExerciseDetailResult.ParticipantInfo createParticipantWithNumber(
            ExerciseDetailResult.ParticipantInfo original,
            int number) {

        return new ExerciseDetailResult.ParticipantInfo(
                original.participantId(),
                number,
                original.profileImageUrl(),
                original.name(),
                original.gender(),
                original.level(),
                original.participantType(),
                original.partyPosition(),
                original.inviterName(),
                original.joinedAt(),
                original.withdrawn()
        );
    }

    private int countByGender(List<ExerciseDetailResult.ParticipantInfo> participants, String gender) {
        return (int) participants.stream()
                .filter(p -> gender.equals(p.gender()))
                .count();
    }

    private record ParticipantGroups(
            List<ExerciseDetailResult.ParticipantInfo> participants,
            List<ExerciseDetailResult.ParticipantInfo> waiting
    ) {
    }
}
