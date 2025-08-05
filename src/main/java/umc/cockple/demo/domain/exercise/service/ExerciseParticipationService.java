package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
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

    private final ExerciseValidator exerciseValidator;

    private final ExerciseConverter exerciseConverter;

    public ExerciseJoinDTO.Response joinExercise(Long exerciseId, Long memberId) {

        log.info("운동 신청 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseWithPartyLevelOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
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

    private boolean isPartyMember(Exercise exercise, Member member) {
        Party party = exercise.getParty();
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    private Exercise findExerciseWithPartyLevelOrThrow(Long exerciseId) {
        return exerciseRepository.findByIdWithPartyLevels(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }


}
