package umc.cockple.demo.domain.exercise.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exercise 도메인")
class ExerciseTest {

    @Nested
    @DisplayName("addParticipation")
    class AddParticipation {

        @Test
        @DisplayName("모임 회원 참여를 완전한 연관관계로 생성한다")
        void partyMemberParticipation() {
            Exercise exercise = exercise();
            Member member = member();

            MemberExercise participation = exercise.addParticipation(
                    member, ExerciseMemberShipStatus.PARTY_MEMBER);

            assertThat(participation.getExercise()).isSameAs(exercise);
            assertThat(participation.getMember()).isSameAs(member);
            assertThat(participation.getExerciseMemberShipStatus())
                    .isEqualTo(ExerciseMemberShipStatus.PARTY_MEMBER);
            assertThat(exercise.getMemberExercises()).containsExactly(participation);
            assertThat(exercise.getNowCapacity()).isEqualTo(1);
        }

        @Test
        @DisplayName("외부 회원 참여 유형을 구분한다")
        void externalMemberParticipation() {
            Exercise exercise = exercise();

            MemberExercise participation = exercise.addParticipation(
                    member(), ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT);

            assertThat(participation.getExerciseMemberShipStatus())
                    .isEqualTo(ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT);
        }
    }

    @Test
    @DisplayName("참여를 제거하면 현재 인원이 감소한다")
    void removeParticipation() {
        Exercise exercise = exercise();
        MemberExercise participation = exercise.addParticipation(
                member(), ExerciseMemberShipStatus.PARTY_MEMBER);

        exercise.removeParticipation(participation);

        assertThat(exercise.getMemberExercises()).doesNotContain(participation);
        assertThat(exercise.getNowCapacity()).isZero();
    }

    private Exercise exercise() {
        return ExerciseFixture.createExercise(null, LocalDate.of(2099, 12, 31));
    }

    private Member member() {
        return MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
    }
}
