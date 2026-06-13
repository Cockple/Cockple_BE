package umc.cockple.demo.domain.exercise.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseBookmarkReader {

    private final ExerciseBookmarkRepository exerciseBookmarkRepository;

    public Map<Long, Boolean> getBookmarkStatus(Long memberId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> bookmarkedExerciseIds = exerciseBookmarkRepository
                .findAllExerciseIdsByMemberIdAndExerciseIds(memberId, exerciseIds);

        return exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        bookmarkedExerciseIds::contains
                ));
    }
}
