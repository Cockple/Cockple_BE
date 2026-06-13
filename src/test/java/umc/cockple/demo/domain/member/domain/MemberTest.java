package umc.cockple.demo.domain.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member 도메인")
class MemberTest {

    @Nested
    @DisplayName("isProfileCompleted")
    class IsProfileCompleted {

        @Test
        @DisplayName("이름_성별_생년월일_급수가_모두_있으면_true를_반환한다")
        void 모든_필수정보가_있으면_true() {
            // given
            Member member = completedMember();

            // when & then
            assertThat(member.isProfileCompleted()).isTrue();
        }

        @Test
        @DisplayName("소셜로그인_직후_상세정보_미입력_회원은_false를_반환한다")
        void 상세정보_미입력이면_false() {
            // given - socialId/nickname만 있는 가입 직후 상태
            Member member = Member.builder()
                    .nickname("kakao_nick")
                    .socialId(1001L)
                    .build();

            // when & then
            assertThat(member.isProfileCompleted()).isFalse();
        }

        @Test
        @DisplayName("memberName만_빠져도_false를_반환한다")
        void memberName이_null이면_false() {
            Member member = completedMember();
            ReflectionTestUtils.setField(member, "memberName", null);

            assertThat(member.isProfileCompleted()).isFalse();
        }

        @Test
        @DisplayName("gender만_빠져도_false를_반환한다")
        void gender가_null이면_false() {
            Member member = completedMember();
            ReflectionTestUtils.setField(member, "gender", null);

            assertThat(member.isProfileCompleted()).isFalse();
        }

        @Test
        @DisplayName("birth만_빠져도_false를_반환한다")
        void birth가_null이면_false() {
            Member member = completedMember();
            ReflectionTestUtils.setField(member, "birth", null);

            assertThat(member.isProfileCompleted()).isFalse();
        }

        @Test
        @DisplayName("level만_빠져도_false를_반환한다")
        void level이_null이면_false() {
            Member member = completedMember();
            ReflectionTestUtils.setField(member, "level", null);

            assertThat(member.isProfileCompleted()).isFalse();
        }

        @Test
        @DisplayName("재가입(initField)으로_필수정보가_초기화되면_다시_false가_된다")
        void 재가입으로_초기화되면_false() {
            // given
            Member member = completedMember();
            assertThat(member.isProfileCompleted()).isTrue();

            // when - rejoin 시 호출되는 필드 초기화
            member.initField();

            // then
            assertThat(member.isProfileCompleted()).isFalse();
        }

        private Member completedMember() {
            return Member.builder()
                    .memberName("강와나")
                    .gender(Gender.FEMALE)
                    .birth(LocalDate.of(2000, 1, 1))
                    .level(Level.A)
                    .nickname("kakao_nick")
                    .socialId(1001L)
                    .build();
        }
    }
}
