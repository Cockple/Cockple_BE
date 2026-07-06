package umc.cockple.demo.global.jwt.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.global.jwt.properties.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider - 토큰 type claim으로 access/refresh 구분")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0LWxvbmctZW5vdWdoLTI1Ng");
        props.setAccessTokenValidity(900000);
        props.setRefreshTokenValidity(1209600000);
        jwtTokenProvider = new JwtTokenProvider(props);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("access 토큰은 isAccessToken=true, isRefreshToken=false 이고 ver/subject를 담는다")
    void accessTokenIsAccessType() {
        String token = jwtTokenProvider.createAccessToken(1L, "nick", 3L);

        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("access");
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getTokenVersion(token)).isEqualTo(3L);
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("refresh 토큰은 isRefreshToken=true, isAccessToken=false")
    void refreshTokenIsRefreshType() {
        String token = jwtTokenProvider.createRefreshToken(1L, "nick", 0L);

        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("refresh");
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("dev 토큰은 access 용도로 발급된다")
    void devTokenIsAccessType() {
        String token = jwtTokenProvider.createDevToken(1L, "nick", 0L);

        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
    }
}
