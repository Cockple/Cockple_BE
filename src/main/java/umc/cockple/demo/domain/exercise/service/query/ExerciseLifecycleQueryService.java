package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantPosition;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseEditDetailResult;
import umc.cockple.demo.domain.exercise.service.support.assembler.ExerciseParticipantSnapshotAssembler;
import umc.cockple.demo.domain.exercise.service.support.calculator.ExerciseParticipantPositionCalculator;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Role;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseLifecycleQueryService {

    private final ExerciseReader exerciseReader;
    private final ExerciseParticipantSnapshotAssembler participantSnapshotAssembler;
    private final ExerciseParticipantPositionCalculator participantPositionCalculator;
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

        List<ExerciseParticipantSnapshot> participants =
                participantSnapshotAssembler.getAllParticipants(exerciseId, party);
        List<ExerciseParticipantPosition> participantPositions =
                participantPositionCalculator.calculate(participants, exercise.getMaxCapacity());
        ParticipantGroups groups = splitParticipants(participantPositions);

        ExerciseDetailResult.ParticipantGroup participantGroup =
                createParticipantGroup(groups.participants(), exercise.getMaxCapacity());
        ExerciseDetailResult.WaitingGroup waitingGroup = createWaitingGroup(groups.waiting());

        return new ExerciseDetailResult(
                isManager, exercise.getGameBoard().getId(), exerciseInfo, participantGroup, waitingGroup);
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
            List<ExerciseParticipantPosition> participantPositions) {

        List<ExerciseDetailResult.ParticipantInfo> participantList = participantPositions.stream()
                .filter(position -> !position.waiting())
                .map(this::toParticipantInfo)
                .toList();
        List<ExerciseDetailResult.ParticipantInfo> waitingList = participantPositions.stream()
                .filter(ExerciseParticipantPosition::waiting)
                .map(this::toParticipantInfo)
                .toList();

        return new ParticipantGroups(participantList, waitingList);
    }

    private ExerciseDetailResult.ParticipantGroup createParticipantGroup(
            List<ExerciseDetailResult.ParticipantInfo> participants,
            int maxCapacity) {

        return new ExerciseDetailResult.ParticipantGroup(
                participants.size(),
                maxCapacity,
                countByGender(participants, Gender.MALE),
                countByGender(participants, Gender.FEMALE),
                participants
        );
    }

    private ExerciseDetailResult.WaitingGroup createWaitingGroup(
            List<ExerciseDetailResult.ParticipantInfo> waiting) {

        return new ExerciseDetailResult.WaitingGroup(
                waiting.size(),
                countByGender(waiting, Gender.MALE),
                countByGender(waiting, Gender.FEMALE),
                waiting
        );
    }

    private ExerciseDetailResult.ParticipantInfo toParticipantInfo(
            ExerciseParticipantPosition position) {
        ExerciseParticipantSnapshot participant = position.participant();

        return new ExerciseDetailResult.ParticipantInfo(
                participant.participantId(),
                position.participantNumber(),
                participant.profileImageUrl(),
                participant.name(),
                participant.gender(),
                participant.level(),
                participant.membershipStatus(),
                participant.partyPosition(),
                participant.inviterName(),
                participant.joinedAt(),
                participant.withdrawn()
        );
    }

    private int countByGender(List<ExerciseDetailResult.ParticipantInfo> participants, Gender gender) {
        return (int) participants.stream()
                .filter(participant -> participant.gender() == gender)
                .count();
    }

    private record ParticipantGroups(
            List<ExerciseDetailResult.ParticipantInfo> participants,
            List<ExerciseDetailResult.ParticipantInfo> waiting
    ) {
    }
}
