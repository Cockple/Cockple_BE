package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseReader")
class ExerciseReaderTest {

    @InjectMocks
    private ExerciseReader exerciseReader;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private Exercise exercise;

    @Test
    @DisplayName("운동 ID로 명령 대상 운동을 조회한다")
    void findByIdOrThrow_returnsExercise() {
        given(exerciseRepository.findById(1L)).willReturn(Optional.of(exercise));

        assertThat(exerciseReader.findByIdOrThrow(1L)).isSameAs(exercise);
    }

    @Test
    @DisplayName("운동이 없으면 ExerciseException(EXERCISE_NOT_FOUND)을 던진다")
    void findByIdOrThrow_throwsWhenMissing() {
        given(exerciseRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseReader.findByIdOrThrow(1L))
                .isInstanceOf(ExerciseException.class)
                .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                        .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }
}
