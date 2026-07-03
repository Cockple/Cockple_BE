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
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.exception.RestAuthenticationEntryPoint;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterMdcTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RestAuthenticationEntryPoint restEntryPoint;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("인증 성공 시 요청 처리 동안 memberId를 MDC에 넣고 종료 후 정리한다")
    void putMemberIdDuringAuthenticatedRequestAndClearAfterwards() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, memberRepository, restEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Member member = Member.builder()
                .id(7L)
                .nickname("member7")
                .isActive(MemberStatus.ACTIVE)
                .socialId(700L)
                .build();
        AtomicReference<String> memberIdInChain = new AtomicReference<>();

        given(jwtTokenProvider.validateToken("token")).willReturn(true);
        given(jwtTokenProvider.getUserId("token")).willReturn(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
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
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, memberRepository, restEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> memberIdInChain = new AtomicReference<>();
        MDC.put(JwtAuthenticationFilter.MEMBER_ID, "stale-member");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                memberIdInChain.set(MDC.get(JwtAuthenticationFilter.MEMBER_ID))
        );

        assertThat(memberIdInChain.get()).isNull();
        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
        verifyNoInteractions(jwtTokenProvider, memberRepository, restEntryPoint);
    }
}
