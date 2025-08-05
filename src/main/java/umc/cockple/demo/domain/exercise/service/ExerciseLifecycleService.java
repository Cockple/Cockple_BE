package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseLifecycleService {

    private final ExerciseRepository exerciseRepository;
    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseConverter exerciseConverter;

    public ExerciseCreateDTO.Response createExercise(Party party, Member member, ExerciseCreateDTO.Request request) {
        exerciseValidator.validateCreateExercise(member.getId(), request, party);

        ExerciseCreateDTO.Command exerciseCommand = exerciseConverter.toCreateCommand(request);
        ExerciseCreateDTO.AddrCommand addrCommand = exerciseConverter.toAddrCreateCommand(request);

        Exercise exercise = party.createExercise(exerciseCommand, addrCommand);
        party.addExercise(exercise);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 생성 완료 - 운동ID: {}", savedExercise.getId());

        return exerciseConverter.toCreateResponse(savedExercise);
    }

    public ExerciseDeleteDTO.Response deleteExercise(Exercise exercise, Member member) {
        exerciseValidator.validateDeleteExercise(exercise, member.getId());

        Party party = exercise.getParty();
        party.removeExercise(exercise);
        exerciseRepository.delete(exercise);

        partyRepository.save(party);

        log.info("운동 삭제 종료 - exerciseId: {}, memberId: {}", exercise.getId(), member.getId());

        return exerciseConverter.toDeleteResponse(exercise);
    }

    public ExerciseUpdateDTO.Response updateExercise(Long exerciseId, Long memberId, ExerciseUpdateDTO.Request request) {

        log.info("운동 업데이트 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        exerciseValidator.validateUpdateExercise(exercise, member, request);

        ExerciseUpdateDTO.Command updateCommand = exerciseConverter.toUpdateCommand(request);
        ExerciseUpdateDTO.AddrCommand addrUpdateCommand = exerciseConverter.toAddrUpdateCommand(request);

        exercise.updateExerciseInfo(updateCommand);
        exercise.updateExerciseAddr(addrUpdateCommand);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 수정 완료 - exerciseId: {}", savedExercise.getId());

        return exerciseConverter.toUpdateResponse(savedExercise);
    }

    // ========== 조회 메서드 ==========

    private Exercise findExerciseOrThrow(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }
}
