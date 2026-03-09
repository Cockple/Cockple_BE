package umc.cockple.demo.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.dto.MemberDetailInfoRequestDTO;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.*;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCommandService")
class MemberCommandServiceTest {

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Mock private MemberRepository memberRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberKeywordRepository memberKeywordRepository;
    @Mock private KakaoOauthService kakaoOauthService;

    private Member normalMember;

    @BeforeEach
    void setUp() {
        normalMember = MemberFixture.createMember("와나", Gender.MALE, Level.C, 9001L);
        ReflectionTestUtils.setField(normalMember, "id", 1L);
    }

    @Nested
    @DisplayName("memberDetailInfo")
    class MemberDetailInfo {

        private MemberDetailInfoRequestDTO requestWithImg;
        private MemberDetailInfoRequestDTO requestWithoutImg;

        @BeforeEach
        void setUp() {
            requestWithImg = MemberDetailInfoRequestDTO.builder()
                    .memberName("강와나")
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(2002, 4, 2))
                    .level(Level.A)
                    .imgKey("profile/test-key.jpg")
                    .keywords(List.of(Keyword.FRIENDSHIP, Keyword.FREE))
                    .build();

            requestWithoutImg = MemberDetailInfoRequestDTO.builder()
                    .memberName("강와나")
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(2002, 4, 2))
                    .level(Level.A)
                    .imgKey(null)
                    .keywords(List.of(Keyword.FRIENDSHIP))
                    .build();
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("imgKey가_있으면_ProfileImg와_함께_회원정보가_업데이트된다")
            void imgKey가_있으면_ProfileImg와_함께_회원정보가_업데이트된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.memberDetailInfo(normalMember.getId(), requestWithImg);

                // then
                then(memberKeywordRepository).should().saveAll(any());

                assertThat(normalMember.getMemberName()).isEqualTo("강와나");
                assertThat(normalMember.getGender()).isEqualTo(Gender.MALE);
                assertThat(normalMember.getBirth()).isEqualTo(LocalDate.of(2002, 4, 2));
                assertThat(normalMember.getLevel()).isEqualTo(Level.A);
                assertThat(normalMember.getProfileImg()).isNotNull();
                assertThat(normalMember.getProfileImg().getImgKey()).isEqualTo("profile/test-key.jpg");
            }

            @Test
            @DisplayName("imgKey가_없으면_ProfileImg_없이_회원정보가_업데이트된다")
            void imgKey가_없으면_ProfileImg_없이_회원정보가_업데이트된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.memberDetailInfo(normalMember.getId(), requestWithoutImg);

                // then
                then(memberKeywordRepository).should().saveAll(any());

                assertThat(normalMember.getMemberName()).isEqualTo("강와나");
                assertThat(normalMember.getLevel()).isEqualTo(Level.A);
                assertThat(normalMember.getProfileImg()).isNull();
            }

            @Test
            @DisplayName("keywords가_정상적으로_저장된다")
            void keywords가_정상적으로_저장된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.memberDetailInfo(normalMember.getId(), requestWithImg);

                // then
                // saveAll 호출 시 keywords 개수가 request와 동일한지 검증
                assertThat(normalMember.getKeywords()).hasSize(requestWithImg.keywords().size());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.memberDetailInfo(999L, requestWithImg))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    // =====================================================================
    // withdrawMember
    // =====================================================================

    @Nested
    @DisplayName("withdrawMember")
    class WithdrawMember {

        @Test
        @DisplayName("과거_운동은_삭제되지_않고_미래_운동만_삭제한다")
        void 과거_운동은_삭제되지_않고_미래_운동만_삭제한다() {
            // given
            given(memberRepository.findById(normalMember.getId())).willReturn(Optional.of(normalMember));

            // when
            memberCommandService.withdrawMember(normalMember.getId());

            // then
            then(memberExerciseRepository).should()
                    .deleteFutureExercisesByMember(eq(normalMember), any(), any());
            then(memberExerciseRepository).should(never())
                    .deleteAll();
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("미래_운동만_삭제되고_모임과_키워드도_삭제된다")
            void 미래_운동만_삭제되고_모임과_키워드도_삭제된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then
                then(memberExerciseRepository).should()
                        .deleteFutureExercisesByMember(eq(normalMember), any(), any());
                then(memberExerciseRepository).should(never()).deleteAll();
                then(memberPartyRepository).should().deleteAllByMember(normalMember);
                then(memberKeywordRepository).should().deleteAllByMember(normalMember);
            }

            @Test
            @DisplayName("탈퇴_후_회원_상태가_INACTIVE가_되고_refreshToken이_null이_된다")
            void 탈퇴_후_회원_상태가_INACTIVE가_되고_refreshToken이_null이_된다() {
                // given
                normalMember.setRefreshToken("existing-refresh-token");
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then
                assertThat(normalMember.getIsActive()).isEqualTo(MemberStatus.INACTIVE);
                assertThat(normalMember.getRefreshToken()).isNull();
            }

            @Test
            @DisplayName("카카오_연결_끊기가_호출된다")
            void 카카오_연결_끊기가_호출된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then
                then(kakaoOauthService).should().unlinkAccess(normalMember);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("이미_탈퇴한_회원이면_ALREADY_WITHDRAW_예외를_던진다")
            void 이미_탈퇴한_회원이면_ALREADY_WITHDRAW_예외를_던진다() {
                // given
                Member withdrawnMember = MemberFixture.createWithdrawnMember("탈퇴회원", "탈퇴닉", 9002L);
                ReflectionTestUtils.setField(withdrawnMember, "id", 2L);

                given(memberRepository.findById(withdrawnMember.getId()))
                        .willReturn(Optional.of(withdrawnMember));

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(withdrawnMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.ALREADY_WITHDRAW);
            }

            @Test
            @DisplayName("활성_모임의_모임장이면_MANAGER_CANNOT_LEAVE_예외를_던진다")
            void 활성_모임의_모임장이면_MANAGER_CANNOT_LEAVE_예외를_던진다() {
                // given
                MemberParty leaderParty = MemberParty.builder()
                        .role(Role.party_MANAGER)
                        .status(MemberPartyStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();
                normalMember.getMemberParties().add(leaderParty);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(normalMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MANAGER_CANNOT_LEAVE);
            }

            @Test
            @DisplayName("활성_모임의_부모임장이면_SUBMANAGER_CANNOT_LEAVE_예외를_던진다")
            void 활성_모임의_부모임장이면_SUBMANAGER_CANNOT_LEAVE_예외를_던진다() {
                // given
                MemberParty subManagerParty = MemberParty.builder()
                        .role(Role.party_SUBMANAGER)
                        .status(MemberPartyStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();
                normalMember.getMemberParties().add(subManagerParty);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(normalMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.SUBMANAGER_CANNOT_LEAVE);
            }

            @Test
            @DisplayName("비활성_모임의_모임장이면_탈퇴가_가능하다")
            void 비활성_모임의_모임장이면_탈퇴가_가능하다() {
                // given: BANNED 상태의 모임이라면 탈퇴 검증을 통과해야 한다
                MemberParty bannedParty = MemberParty.builder()
                        .role(Role.party_MANAGER)
                        .status(MemberPartyStatus.BANNED)
                        .joinedAt(LocalDateTime.now())
                        .build();
                normalMember.getMemberParties().add(bannedParty);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then: 예외 없이 탈퇴 처리됨
                assertThat(normalMember.getIsActive()).isEqualTo(MemberStatus.INACTIVE);
            }
        }
    }
}
