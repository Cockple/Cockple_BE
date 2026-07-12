package umc.cockple.demo.domain.bookmark.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseBookmarkLookupService")
class ExerciseBookmarkLookupServiceTest {

    @InjectMocks
    private ExerciseBookmarkLookupService exerciseBookmarkLookupService;

    @Mock
    private ExerciseBookmarkRepository exerciseBookmarkRepository;

    @Nested
    @DisplayName("getBookmarkStatus - 운동 ID 목록 기준 북마크 여부 조회")
    class GetBookmarkStatus {

        @Test
        @DisplayName("운동 ID 목록이 비어있으면 빈 Map을 반환하고 Repository를 호출하지 않는다")
        void emptyExerciseIds_returnsEmptyMapWithoutRepositoryCall() {
            // when
            Map<Long, Boolean> result = exerciseBookmarkLookupService.getBookmarkStatus(1L, List.of());

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(exerciseBookmarkRepository);
        }

        @Test
        @DisplayName("요청한 운동 ID 전체에 대해 북마크 여부 Map을 반환한다")
        void repositoryBookmarkIds_returnsBookmarkStatusMap() {
            // given
            Long memberId = 1L;
            List<Long> exerciseIds = List.of(10L, 20L, 30L);
            given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(memberId, exerciseIds))
                    .willReturn(List.of(10L, 30L));

            // when
            Map<Long, Boolean> result = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);

            // then
            assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                    10L, true,
                    20L, false,
                    30L, true
            ));
            verify(exerciseBookmarkRepository).findAllExerciseIdsByMemberIdAndExerciseIds(memberId, exerciseIds);
        }
    }
}
