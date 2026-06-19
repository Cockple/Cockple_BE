package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.query.lookup.ExerciseParticipantCountLookupService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseParticipantCountLookupService")
class ExerciseParticipantCountLookupServiceTest {

    @InjectMocks
    private ExerciseParticipantCountLookupService exerciseParticipantCountLookupService;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Nested
    @DisplayName("getParticipantCountsByExerciseIds - 운동 ID 목록 기준 참여 인원 수 조회")
    class GetParticipantCountsByExerciseIds {

        @Test
        @DisplayName("운동 ID 목록이 비어있으면 빈 Map을 반환하고 Repository를 호출하지 않는다")
        void emptyExerciseIds_returnsEmptyMapWithoutRepositoryCall() {
            // when
            Map<Long, Integer> result = exerciseParticipantCountLookupService
                    .getParticipantCountsByExerciseIds(List.of());

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(exerciseRepository);
        }

        @Test
        @DisplayName("Repository count 결과를 운동 ID별 참여 인원 수 Map으로 변환한다")
        void repositoryCountRows_returnsParticipantCountMap() {
            // given
            List<Long> exerciseIds = List.of(1L, 2L);
            given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(exerciseIds))
                    .willReturn(List.of(
                            new Object[]{1L, 3L},
                            new Object[]{2L, 5L}
                    ));

            // when
            Map<Long, Integer> result = exerciseParticipantCountLookupService
                    .getParticipantCountsByExerciseIds(exerciseIds);

            // then
            assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                    1L, 3,
                    2L, 5
            ));
            verify(exerciseRepository).findExerciseParticipantCountsByExerciseIds(exerciseIds);
        }
    }

    @Nested
    @DisplayName("getParticipantCountsByExerciseIdsAndDateRange - 운동 ID 목록과 날짜 범위 기준 참여 인원 수 조회")
    class GetParticipantCountsByExerciseIdsAndDateRange {

        @Test
        @DisplayName("운동 ID 목록이 비어있으면 빈 Map을 반환하고 Repository를 호출하지 않는다")
        void emptyExerciseIds_returnsEmptyMapWithoutRepositoryCall() {
            // when
            Map<Long, Integer> result = exerciseParticipantCountLookupService
                    .getParticipantCountsByExerciseIdsAndDateRange(
                            List.of(),
                            LocalDate.of(2026, 6, 1),
                            LocalDate.of(2026, 6, 30)
                    );

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(exerciseRepository);
        }

        @Test
        @DisplayName("Repository count 결과를 운동 ID별 참여 인원 수 Map으로 변환한다")
        void repositoryCountRows_returnsParticipantCountMap() {
            // given
            List<Long> exerciseIds = List.of(1L, 2L);
            LocalDate start = LocalDate.of(2026, 6, 1);
            LocalDate end = LocalDate.of(2026, 6, 30);
            given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(exerciseIds, start, end))
                    .willReturn(List.of(
                            new Object[]{1L, 4L},
                            new Object[]{2L, 6L}
                    ));

            // when
            Map<Long, Integer> result = exerciseParticipantCountLookupService
                    .getParticipantCountsByExerciseIdsAndDateRange(exerciseIds, start, end);

            // then
            assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                    1L, 4,
                    2L, 6
            ));
            verify(exerciseRepository).findExerciseParticipantCountsByExerciseIds(exerciseIds, start, end);
        }
    }

    @Nested
    @DisplayName("getParticipantCountsByPartyIdAndDateRange - 모임과 날짜 범위 기준 참여 인원 수 조회")
    class GetParticipantCountsByPartyIdAndDateRange {

        @Test
        @DisplayName("Repository count 결과를 운동 ID별 참여 인원 수 Map으로 변환한다")
        void repositoryCountRows_returnsParticipantCountMap() {
            // given
            Long partyId = 10L;
            LocalDate start = LocalDate.of(2026, 6, 1);
            LocalDate end = LocalDate.of(2026, 6, 30);
            given(exerciseRepository.findExerciseParticipantCounts(partyId, start, end))
                    .willReturn(List.of(
                            new Object[]{1L, 7L},
                            new Object[]{2L, 8L}
                    ));

            // when
            Map<Long, Integer> result = exerciseParticipantCountLookupService
                    .getParticipantCountsByPartyIdAndDateRange(partyId, start, end);

            // then
            assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                    1L, 7,
                    2L, 8
            ));
            verify(exerciseRepository).findExerciseParticipantCounts(partyId, start, end);
        }
    }
}
