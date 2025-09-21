package umc.cockple.demo.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.exception.RestAuthenticationEntryPoint;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;
import umc.cockple.demo.global.security.domain.CustomUserDetails;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RestAuthenticationEntryPoint restEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

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


            Long memberId = jwtTokenProvider.getUserId(token);
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

            // 탈퇴 회원 검증
            if (member.getIsActive() == MemberStatus.INACTIVE) {
                throw new MemberException(MemberErrorCode.ALREADY_WITHDRAW);
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

    }



    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

}
