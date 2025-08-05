package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseParticipationService {

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final GuestRepository guestRepository;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseConverter exerciseConverter;

    public ExerciseJoinDTO.Response joinExercise(Exercise exercise, Member member) {
        exerciseValidator.validateJoinExercise(exercise, member);

        boolean isPartyMember = isPartyMember(exercise, member);
        MemberExercise memberExercise = MemberExercise.create(isPartyMember);
        member.addParticipation(memberExercise);
        exercise.addParticipation(memberExercise);

        MemberExercise savedMemberExercise = memberExerciseRepository.save(memberExercise);

        log.info("운동 신청 종료 - memberExerciseId: {}, isPartyMember : {}"
                , savedMemberExercise.getId(), isPartyMember);

        return exerciseConverter.toJoinResponse(savedMemberExercise, exercise);
    }

    public ExerciseCancelDTO.Response cancelParticipation(Long exerciseId, Long memberId) {

        log.info("운동 참여 취소 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        MemberExercise memberExercise = findMemberExerciseOrThrow(exercise, member);
        exerciseValidator.validateCancelParticipation(exercise);

        exercise.removeParticipation(memberExercise);
        member.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);

        exerciseRepository.save(exercise);

        log.info("운동 참여 취소 완료 - exerciseId: {}, memberId: {}, 현재 참여자 수: {}",
                exerciseId, memberId, exercise.getNowCapacity());

        return exerciseConverter.toCancelResponse(exercise, member);
    }

    public ExerciseCancelDTO.Response cancelParticipationByManager(
            Long exerciseId, Long participantId, Long memberId, ExerciseCancelDTO.ByManagerRequest request) {

        log.info("매니저에 의한 운동 참여 취소 시작 - exerciseId: {}, participantId: {}, memberId: {}", exerciseId, participantId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member manager = findMemberOrThrow(memberId);
        exerciseValidator.validateCancelCommonParticipationByManager(exercise, manager);

        ExerciseCancelDTO.Response response = executeParticipantCancellation(exercise, participantId, request);

        log.info("매니저에 의한 운동 참여 취소 완료 - exerciseId: {}, participantId: {}, 현재 참여자 수: {}",
                exerciseId, participantId, exercise.getNowCapacity());

        return response;
    }

    // ========== 비즈니스 메서드 ============

    private boolean isPartyMember(Exercise exercise, Member member) {
        Party party = exercise.getParty();
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    private ExerciseCancelDTO.Response executeParticipantCancellation(Exercise exercise, Long participantId, ExerciseCancelDTO.ByManagerRequest request) {
        if(request.isGuest()){
            log.info("게스트 참여 취소 실행 - participantId: {}", participantId);
            return cancelGuestParticipation(exercise, participantId);
        }

        log.info("멤버 참여 취소 실행 - participantId: {}", participantId);
        return cancelMemberParticipation(exercise, participantId);
    }

    private ExerciseCancelDTO.Response cancelGuestParticipation(Exercise exercise, Long participantId) {
        Guest guest = findGuestOrThrow(participantId);
        exerciseValidator.validateCancelGuestParticipationByManager(guest, exercise);

        exercise.removeGuest(guest);

        guestRepository.delete(guest);

        exerciseRepository.save(exercise);

        return exerciseConverter.toCancelResponse(exercise, guest);
    }

    private ExerciseCancelDTO.Response cancelMemberParticipation(Exercise exercise, Long participantId) {
        Member participant = findMemberOrThrow(participantId);
        MemberExercise memberExercise = findMemberExerciseOrThrow(exercise, participant);

        exercise.removeParticipation(memberExercise);
        participant.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);

        exerciseRepository.save(exercise);

        return exerciseConverter.toCancelResponse(exercise, participant);
    }

    // ========== 조회 메서드 ============

    private Exercise findExerciseOrThrow(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Guest findGuestOrThrow(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.GUEST_NOT_FOUND));
    }

    private Exercise findExerciseWithPartyLevelOrThrow(Long exerciseId) {
        return exerciseRepository.findByIdWithPartyLevels(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberExercise findMemberExerciseOrThrow(Exercise exercise, Member member) {
        return memberExerciseRepository.findByExerciseAndMember(exercise, member)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND));
    }
}
