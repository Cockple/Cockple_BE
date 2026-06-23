package umc.cockple.demo.domain.exercise.service.query.lookup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseParticipantCountLookupService {

    private final ExerciseRepository exerciseRepository;

    public Map<Long, Integer> getParticipantCountsByPartyIdAndDateRange(
            Long partyId,
            LocalDate start,
            LocalDate end) {
        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCounts(partyId, start, end);
        return toCountMap(countResults);
    }

    public Map<Long, Integer> getParticipantCountsByExerciseIdsAndDateRange(
            List<Long> exerciseIds,
            LocalDate start,
            LocalDate end) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                exerciseIds, start, end);
        return toCountMap(countResults);
    }

    public Map<Long, Integer> getParticipantCountsByExerciseIds(List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCountsByExerciseIds(exerciseIds);
        return toCountMap(countResults);
    }

    private Map<Long, Integer> toCountMap(List<Object[]> countResults) {
        return countResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));
    }
}
