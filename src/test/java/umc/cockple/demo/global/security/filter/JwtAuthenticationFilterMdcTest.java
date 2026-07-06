package umc.cockple.demo.global.security.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import umc.cockple.demo.global.exception.RestAuthenticationEntryPoint;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterMdcTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RestAuthenticationEntryPoint restEntryPoint;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("인증 성공 시 요청 처리 동안 memberId를 MDC에 넣고 종료 후 정리한다")
    void putMemberIdDuringAuthenticatedRequestAndClearAfterwards() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, restEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> memberIdInChain = new AtomicReference<>();

        given(jwtTokenProvider.validateToken("token")).willReturn(true);
        given(jwtTokenProvider.isAccessToken("token")).willReturn(true);
        given(jwtTokenProvider.getUserId("token")).willReturn(7L);
        given(jwtTokenProvider.getAuthentication("token"))
                .willReturn(new UsernamePasswordAuthenticationToken("member7", ""));

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                memberIdInChain.set(MDC.get(JwtAuthenticationFilter.MEMBER_ID))
        );

        assertThat(memberIdInChain.get()).isEqualTo("7");
        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
    }

    @Test
    @DisplayName("토큰이 없는 요청은 이전 memberId MDC를 제거한 상태로 통과시키고 종료 후에도 정리한다")
    void clearStaleMemberIdWhenRequestHasNoToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, restEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> memberIdInChain = new AtomicReference<>();
        MDC.put(JwtAuthenticationFilter.MEMBER_ID, "stale-member");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                memberIdInChain.set(MDC.get(JwtAuthenticationFilter.MEMBER_ID))
        );

        assertThat(memberIdInChain.get()).isNull();
        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
        verifyNoInteractions(jwtTokenProvider, restEntryPoint);
    }

    @Test
    @DisplayName("토큰이 없는 요청의 downstream 예외는 인증 실패로 변환하지 않고 전파한다")
    void propagateDownstreamExceptionWhenRequestHasNoToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, restEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put(JwtAuthenticationFilter.MEMBER_ID, "stale-member");

        assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new IllegalStateException("downstream failure");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("downstream failure");

        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
        verifyNoInteractions(jwtTokenProvider, restEntryPoint);
    }

    @Test
    @DisplayName("refresh 토큰으로 일반 API에 접근하면 401로 거부하고 체인을 진행하지 않는다")
    void rejectRefreshTokenOnApiRequest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, restEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        given(jwtTokenProvider.validateToken("refresh")).willReturn(true);
        given(jwtTokenProvider.isAccessToken("refresh")).willReturn(false); // refresh 타입

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isFalse();
        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
        verify(restEntryPoint).commence(any(), any(), any());
    }
}
