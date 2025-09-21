package umc.cockple.demo.global.jwt.domain;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.jwt.properties.JwtProperties;
import umc.cockple.demo.global.security.domain.CustomUserDetails;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    private Key key;
    private final JwtProperties jwtProperties;
    private final MemberRepository memberRepository;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64URL.decode(jwtProperties.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long memberId, String nickname) {
        return createToken(memberId, nickname, jwtProperties.getAccessTokenValidity());
    }

    public String createRefreshToken(Long memberId, String nickname) {
        return createToken(memberId, nickname, jwtProperties.getRefreshTokenValidity());
    }

    public String createDevToken(Long memberId, String nickname) {
        return createToken(memberId, nickname, 1209600000L * 2);
    }

    private String createToken(Long memberId, String nickname, long validity) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(memberId));

        if (nickname == null) {
            throw new MemberException(MemberErrorCode.NICKNAME_IS_NULL);
        }

        claims.put("nickname", nickname);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        // 토큰 null 처리
        if (token == null || token.isBlank()) {
            log.warn("JWT validateToken: token is null or blank");
            throw new MemberException(MemberErrorCode.JWT_IS_NULL);
        }

        // 접두어가 섞여 들어오는 경우 처리
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
            if (token.isEmpty()) {
                log.warn("JWT validateToken: 'Bearer ' prefix present but no token");
                throw new MemberException(MemberErrorCode.JWT_IS_NULL);
            }
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (io.jsonwebtoken.security.SignatureException exception) {
            log.error("JWT signature validation fails");
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        } catch (Exception exception) {
            log.error("JWT validation fails", exception);
        }
        return false;
    }


    public boolean isTokenExpiringSoon(String refreshToken, long thresholdMillis) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();

            return expiration.getTime() - now < thresholdMillis;
        } catch (JwtException e) {
            throw new MemberException(MemberErrorCode.INVALID_TOKEN);
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.valueOf(claims.getSubject());
    }

    public Authentication getAuthentication(String token) {
        Long memberId = getUserId(token);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        UserDetails userDetails = new CustomUserDetails(member.getId(), member.getNickname());
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}
