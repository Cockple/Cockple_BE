package umc.cockple.demo.domain.exercise.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseParticipantReader {

    private final MemberExerciseRepository memberExerciseRepository;

    public MemberExercise findMemberExerciseOrThrow(Exercise exercise, Member member) {
        return memberExerciseRepository.findByExerciseAndMember(exercise, member)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND));
    }

    public List<MemberExercise> findMemberExercisesWithMemberAndProfile(Long exerciseId) {
        return memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exerciseId);
    }

    public Map<Long, Boolean> getParticipatingStatus(Long memberId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> participatingExerciseIds = memberExerciseRepository
                .findAllExerciseIdsByMemberAndExerciseIds(memberId, exerciseIds);
        Set<Long> participatingExerciseIdSet = new HashSet<>(participatingExerciseIds);

        return exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        participatingExerciseIdSet::contains
                ));
    }
}
