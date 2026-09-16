package umc.cockple.demo.domain.exercise.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseParticipation;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseParticipationRepository;
import umc.cockple.demo.domain.member.domain.Member;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseParticipationReader {

    private final ExerciseParticipationRepository exerciseParticipationRepository;

    public ExerciseParticipation findExerciseParticipationOrThrow(Exercise exercise, Member member) {
        return exerciseParticipationRepository.findByExerciseAndMember(exercise, member)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_PARTICIPATION_NOT_FOUND));
    }

    public List<ExerciseParticipation> findExerciseParticipationsWithMemberAndProfile(Long exerciseId) {
        return exerciseParticipationRepository.findByExerciseIdWithMemberAndProfile(exerciseId);
    }

    public Map<Long, LocalDate> findLastExerciseDates(
            List<Long> memberIds, Long partyId) {
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return exerciseParticipationRepository
                .findLastExerciseDateByMemberIdsAndPartyId(memberIds, partyId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (LocalDate) row[1]
                ));
    }

    public Map<Long, Boolean> getParticipatingStatus(Long memberId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> participatingExerciseIds = exerciseParticipationRepository
                .findAllExerciseIdsByMemberAndExerciseIds(memberId, exerciseIds);
        Set<Long> participatingExerciseIdSet = new HashSet<>(participatingExerciseIds);

        return exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        participatingExerciseIdSet::contains
                ));
    }
}
