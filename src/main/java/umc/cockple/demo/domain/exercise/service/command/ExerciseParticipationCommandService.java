package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.command.ExerciseParticipationCommandMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseParticipationCommandService {

    private final MemberExerciseRepository memberExerciseRepository;
    private final GuestRepository guestRepository;
    private final ExerciseReader exerciseReader;
    private final GuestReader guestReader;
    private final ExerciseParticipantReader exerciseParticipantReader;
    private final MemberLookupService memberLookupService;
    private final MemberPartyLookupService memberPartyLookupService;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseParticipationCommandMapper exerciseParticipationMapper;

    public ExerciseJoinDTO.Response joinExercise(Long exerciseId, Long memberId) {
        log.info("운동 신청 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdWithPartyLevelsOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateJoinExercise(exercise, member);

        boolean isPartyMember = memberPartyLookupService.isPartyMember(exercise.getParty(), member);
        MemberExercise memberExercise = MemberExercise.create(isPartyMember);
        member.addParticipation(memberExercise);
        exercise.addParticipation(memberExercise);

        MemberExercise savedMemberExercise = memberExerciseRepository.save(memberExercise);

        log.info("운동 신청 종료 - memberExerciseId: {}, isPartyMember : {}"
                , savedMemberExercise.getId(), isPartyMember);

        return exerciseParticipationMapper.toJoinResponse(savedMemberExercise, exercise);
    }

    public ExerciseCancelDTO.Response cancelParticipation(Long exerciseId, Long memberId) {
        log.info("운동 참여 취소 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);
        MemberExercise memberExercise = exerciseParticipantReader.findMemberExerciseOrThrow(exercise, member);
        exerciseValidator.validateCancelParticipation(exercise);

        exercise.removeParticipation(memberExercise);
        member.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);

        log.info("운동 참여 취소 완료 - exerciseId: {}, memberId: {}, 현재 참여자 수: {}",
                exercise.getId(), member.getId(), exercise.getNowCapacity());

        return exerciseParticipationMapper.toCancelResponse(exercise, member);
    }

    public ExerciseCancelDTO.Response cancelParticipationByManager(
            Long exerciseId, Long participantId, Long managerId, ExerciseCancelDTO.ByManagerRequest request) {
        log.info("매니저에 의한 운동 참여 취소 시작 - exerciseId: {}, participantId: {}, memberId: {}",
                exerciseId, participantId, managerId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member manager = memberLookupService.findByIdOrThrow(managerId);

        exerciseValidator.validateCancelCommonParticipationByManager(exercise, manager);

        ExerciseCancelDTO.Response response = executeParticipantCancellation(exercise, participantId, request);

        log.info("매니저에 의한 운동 참여 취소 완료 - exerciseId: {}, participantId: {}, 현재 참여자 수: {}",
                exercise.getId(), participantId, exercise.getNowCapacity());

        return response;
    }

    // ========== 비즈니스 메서드 ============

    private ExerciseCancelDTO.Response executeParticipantCancellation(Exercise exercise, Long participantId, ExerciseCancelDTO.ByManagerRequest request) {
        if(request.isGuest()){
            log.info("게스트 참여 취소 실행 - participantId: {}", participantId);
            return cancelGuestParticipation(exercise, participantId);
        }

        log.info("멤버 참여 취소 실행 - participantId: {}", participantId);
        return cancelMemberParticipation(exercise, participantId);
    }

    private ExerciseCancelDTO.Response cancelGuestParticipation(Exercise exercise, Long participantId) {
        Guest guest = guestReader.findByIdOrThrow(participantId);
        exerciseValidator.validateCancelGuestParticipationByManager(guest, exercise);

        exercise.removeGuest(guest);

        guestRepository.delete(guest);

        return exerciseParticipationMapper.toCancelResponse(exercise, guest);
    }

    private ExerciseCancelDTO.Response cancelMemberParticipation(Exercise exercise, Long participantId) {
        Member participant = memberLookupService.findByIdOrThrow(participantId);
        MemberExercise memberExercise = exerciseParticipantReader.findMemberExerciseOrThrow(exercise, participant);

        exercise.removeParticipation(memberExercise);
        participant.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);

        return exerciseParticipationMapper.toCancelResponse(exercise, participant);
    }
}
