package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseGuestService;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseLifecycleService;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseParticipationService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseCommandService {

    private final ExerciseLifecycleService exerciseLifecycleService;
    private final ExerciseParticipationService exerciseParticipationService;
    private final ExerciseGuestService exerciseGuestService;

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;
    private final ExerciseRepository exerciseRepository;
    private final GuestRepository guestRepository;

    public ExerciseCreateDTO.Response createExercise(Long partyId, Long memberId, ExerciseCreateDTO.Request request) {
        log.info("운동 생성 시작 - partyId: {}, memberId: {}, date: {}", partyId, memberId, request.date());

        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        return exerciseLifecycleService.createExercise(party, member, request);
    }

    public ExerciseDeleteDTO.Response deleteExercise(Long exerciseId, Long memberId) {
        log.info("운동 삭제 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        return exerciseLifecycleService.deleteExercise(exercise, member);
    }

    public ExerciseUpdateDTO.Response updateExercise(Long exerciseId, Long memberId, ExerciseUpdateDTO.Request request) {
        log.info("운동 업데이트 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        return exerciseLifecycleService.updateExercise(exercise, member, request);
    }

    public ExerciseJoinDTO.Response joinExercise(Long exerciseId, Long memberId) {
        log.info("운동 신청 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseWithPartyLevelOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        return exerciseParticipationService.joinExercise(exercise, member);
    }

    public ExerciseCancelDTO.Response cancelParticipation(Long exerciseId, Long memberId) {
        log.info("운동 참여 취소 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        return exerciseParticipationService.cancelParticipation(exercise, member);
    }

    public ExerciseCancelDTO.Response cancelParticipationByManager(
            Long exerciseId, Long participantId, Long managerId, ExerciseCancelDTO.ByManagerRequest request) {
        log.info("매니저에 의한 운동 참여 취소 시작 - exerciseId: {}, participantId: {}, memberId: {}"
                , exerciseId, participantId, managerId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member manager = findMemberOrThrow(managerId);

        return exerciseParticipationService.cancelParticipationByManager(exercise, participantId, manager, request);
    }

    public ExerciseGuestInviteDTO.Response inviteGuest(Long exerciseId, Long inviterId, ExerciseGuestInviteDTO.Request request) {
        log.info("게스트 초대 시작 - exerciseId: {}, inviterId: {}, guestName: {}"
                , exerciseId, inviterId, request.guestName());

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member inviter = findMemberOrThrow(inviterId);

        return exerciseGuestService.inviteGuest(exercise, inviter, request);
    }

    public ExerciseCancelDTO.Response cancelGuestInvitation(Long exerciseId, Long guestId, Long memberId) {
        log.info("게스트 초대 취소 시작 - exerciseId: {}, guestId: {}, memberId: {}", exerciseId, guestId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        Guest guest = findGuestOrThrow(guestId);

        return exerciseGuestService.cancelGuestInvitation(exercise, guest, member);
    }

    // ========== 조회 메서드 ==========

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }

    private Exercise findExerciseOrThrow(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Exercise findExerciseWithPartyLevelOrThrow(Long exerciseId) {
        return exerciseRepository.findByIdWithPartyLevels(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Guest findGuestOrThrow(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.GUEST_NOT_FOUND));
    }
}
