package umc.cockple.demo.domain.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.enums.MemberStatus;
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

    @Nested
    @DisplayName("rejoin")
    class Rejoin {

        @Test
        @DisplayName("탈퇴한_회원이_재가입하면_활성화되고_탈퇴시각이_초기화되며_프로필은_보존된다")
        void 재가입하면_활성화되고_프로필은_보존된다() {
            // given - 상세정보까지 입력했다가 탈퇴한 회원
            Member member = Member.builder()
                    .memberName("강와나")
                    .gender(Gender.FEMALE)
                    .birth(LocalDate.of(2000, 1, 1))
                    .level(Level.A)
                    .nickname("kakao_nick")
                    .socialId(1001L)
                    .isActive(MemberStatus.ACTIVE)
                    .build();
            member.withdraw();
            assertThat(member.isWithdrawn()).isTrue();
            assertThat(member.getDeletedAt()).isNotNull();

            // when
            member.rejoin();

            // then - 계정이 복원되고 기존 프로필이 유지되어 온보딩 없이 홈으로 진입한다
            assertThat(member.getIsActive()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getDeletedAt()).isNull();
            assertThat(member.isProfileCompleted()).isTrue();
        }
    }
}
