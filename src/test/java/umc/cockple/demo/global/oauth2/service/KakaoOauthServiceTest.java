package umc.cockple.demo.global.oauth2.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.auth.RefreshTokenRepository;
import umc.cockple.demo.global.auth.TokenVersionRepository;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.jwt.domain.TokenRefreshResponse;
import umc.cockple.demo.global.oauth2.domain.KakaoClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoOauthService.validateMember - 리프레시 토큰 재사용 탐지")
class KakaoOauthServiceTest {

    @InjectMocks
    private KakaoOauthService kakaoOauthService;

    @Mock private KakaoClient kakaoClient;
    @Mock private MemberRepository memberRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenVersionRepository tokenVersionRepository;

    private static final String RT = "refresh.token.value";
    private static final Long MEMBER_ID = 1L;

    @Nested
    @DisplayName("정상 회전")
    class NormalRotation {

        @Test
        @DisplayName("활성 토큰이면 소비 이력을 남기고 새 토큰을 재발급한다")
        void 정상_회전() {
            // given
            Member member = mock(Member.class);
            given(member.getId()).willReturn(MEMBER_ID);
            given(member.getNickname()).willReturn("와나");
            given(member.getIsActive()).willReturn(MemberStatus.ACTIVE);

            given(refreshTokenRepository.consumeAndMark(RT)).willReturn(Optional.of(MEMBER_ID));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(tokenVersionRepository.getVersion(MEMBER_ID)).willReturn(0L);
            given(jwtTokenProvider.getTokenVersion(RT)).willReturn(0L);
            given(jwtTokenProvider.createAccessToken(eq(MEMBER_ID), any(), eq(0L))).willReturn("newAccess");
            given(jwtTokenProvider.createRefreshToken(eq(MEMBER_ID), any(), eq(0L))).willReturn("newRefresh");

            // when
            TokenRefreshResponse response = kakaoOauthService.validateMember(RT);

            // then
            assertThat(response.accessToken()).isEqualTo("newAccess");
            assertThat(response.refreshToken()).isEqualTo("newRefresh");
            verify(refreshTokenRepository).consumeAndMark(RT);
            verify(refreshTokenRepository).save("newRefresh", MEMBER_ID);
            verify(tokenVersionRepository, never()).increment(anyLong());
        }
    }

    @Nested
    @DisplayName("활성 저장소에 없는 토큰")
    class TokenNotInStore {

        @Test
        @DisplayName("서명 유효 + grace 경과 → 재사용으로 판정하고 전체 토큰을 무효화한다")
        void 재사용_확정() {
            // given
            given(refreshTokenRepository.consumeAndMark(RT)).willReturn(Optional.empty());
            given(jwtTokenProvider.validateToken(RT)).willReturn(true);
            given(jwtTokenProvider.isRefreshToken(RT)).willReturn(true);
            given(refreshTokenRepository.isRecentlyConsumed(RT)).willReturn(false);
            given(jwtTokenProvider.getUserId(RT)).willReturn(MEMBER_ID);

            // when & then
            assertThatThrownBy(() -> kakaoOauthService.validateMember(RT))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("code", MemberErrorCode.INVALID_REFRESH_TOKEN);

            verify(tokenVersionRepository).increment(MEMBER_ID);
        }

        @Test
        @DisplayName("grace window 이내 소비 이력이 있으면(동시요청/재시도) 무효화하지 않는다")
        void grace_이내_재시도() {
            // given
            given(refreshTokenRepository.consumeAndMark(RT)).willReturn(Optional.empty());
            given(jwtTokenProvider.validateToken(RT)).willReturn(true);
            given(jwtTokenProvider.isRefreshToken(RT)).willReturn(true);
            given(refreshTokenRepository.isRecentlyConsumed(RT)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> kakaoOauthService.validateMember(RT))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("code", MemberErrorCode.INVALID_REFRESH_TOKEN);

            verify(tokenVersionRepository, never()).increment(anyLong());
        }

        @Test
        @DisplayName("서명 무효/만료 토큰이면 무효화하지 않고 일반 거부한다")
        void 만료_또는_무효_토큰() {
            // given
            given(refreshTokenRepository.consumeAndMark(RT)).willReturn(Optional.empty());
            given(jwtTokenProvider.validateToken(RT)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> kakaoOauthService.validateMember(RT))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("code", MemberErrorCode.INVALID_REFRESH_TOKEN);

            verify(tokenVersionRepository, never()).increment(anyLong());
        }
    }
}
