package umc.cockple.demo.domain.bookmark.service.query.lookup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseBookmarkLookupService {

    private final ExerciseBookmarkRepository exerciseBookmarkRepository;

    public Map<Long, Boolean> getBookmarkStatus(Long memberId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> bookmarkedExerciseIds = exerciseBookmarkRepository
                .findAllExerciseIdsByMemberIdAndExerciseIds(memberId, exerciseIds);
        Set<Long> bookmarkedExerciseIdSet = new HashSet<>(bookmarkedExerciseIds);

        return exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        bookmarkedExerciseIdSet::contains
                ));
    }
}
