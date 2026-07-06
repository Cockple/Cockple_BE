package umc.cockple.demo.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.global.auth.TokenVersionRepository;
import umc.cockple.demo.global.exception.RestAuthenticationEntryPoint;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String MEMBER_ID = "memberId";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenVersionRepository tokenVersionRepository;
    private final RestAuthenticationEntryPoint restEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        MDC.remove(MEMBER_ID);
        try {
            String token = resolveToken(request);
            // token이 null -> 로그 찍고 그대로 진행
            if (token == null) {
                log.trace("Authorization 헤더에 토큰 없음");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    throw new MemberException(MemberErrorCode.INVALID_TOKEN);
                }

                // refresh 토큰을 일반 API 인증에 사용하는 것을 차단
                if (!jwtTokenProvider.isAccessToken(token)) {
                    throw new MemberException(MemberErrorCode.INVALID_TOKEN);
                }

                Long memberId = jwtTokenProvider.getUserId(token);
                MDC.put(MEMBER_ID, String.valueOf(memberId));

                // 토큰 버전 검증 - 강제 무효화(탈퇴/탈취 대응 등)된 토큰 차단 (DB 조회 없이 Redis version만 확인)
                long currentVersion = tokenVersionRepository.getVersion(memberId);
                if (jwtTokenProvider.getTokenVersion(token) != currentVersion) {
                    throw new MemberException(MemberErrorCode.INVALID_TOKEN);
                }

                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("인증 정보 SecurityContext에 저장 완료: {}", auth.getName());

                filterChain.doFilter(request, response);

            } catch (MemberException e) {
                SecurityContextHolder.clearContext();
                restEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage() == null ? "UNAUTHORIZED" : e.getMessage()));

            } catch (RuntimeException e) { // 혹시 남아있는 경우에도 401로 변환
                SecurityContextHolder.clearContext();
                restEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage() == null ? "UNAUTHORIZED" : e.getMessage()));
            }
        } finally {
            MDC.remove(MEMBER_ID);
        }

    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
