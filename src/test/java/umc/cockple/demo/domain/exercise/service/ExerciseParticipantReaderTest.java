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
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipantReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseParticipantReader")
class ExerciseParticipantReaderTest {

    @InjectMocks
    private ExerciseParticipantReader exerciseParticipantReader;

    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private Exercise exercise;
    @Mock private Member member;
    @Mock private MemberExercise memberExercise;

    @Test
    @DisplayName("운동과 멤버로 참가 기록을 조회한다")
    void findMemberExerciseOrThrow_returnsMemberExercise() {
        given(memberExerciseRepository.findByExerciseAndMember(exercise, member))
                .willReturn(Optional.of(memberExercise));

        assertThat(exerciseParticipantReader.findMemberExerciseOrThrow(exercise, member))
                .isSameAs(memberExercise);
    }

    @Test
    @DisplayName("참가 기록이 없으면 ExerciseException(MEMBER_EXERCISE_NOT_FOUND)을 던진다")
    void findMemberExerciseOrThrow_throwsWhenMissing() {
        given(memberExerciseRepository.findByExerciseAndMember(exercise, member))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseParticipantReader.findMemberExerciseOrThrow(exercise, member))
                .isInstanceOf(ExerciseException.class)
                .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                        .isEqualTo(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND));
    }
}
