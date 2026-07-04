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

    public String createAccessToken(Long memberId, String nickname, long tokenVersion) {
        return createToken(memberId, nickname, tokenVersion, jwtProperties.getAccessTokenValidity());
    }

    public String createRefreshToken(Long memberId, String nickname, long tokenVersion) {
        return createToken(memberId, nickname, tokenVersion, jwtProperties.getRefreshTokenValidity());
    }

    public String createDevToken(Long memberId, String nickname, long tokenVersion) {
        return createToken(memberId, nickname, tokenVersion, 1209600000L * 2);
    }

    private String createToken(Long memberId, String nickname, long tokenVersion, long validity) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(memberId));

        if (nickname == null) {
            throw new MemberException(MemberErrorCode.NICKNAME_IS_NULL);
        }

        claims.put("nickname", nickname);
        claims.put("ver", tokenVersion);

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
            log.debug("Invalid JWT Token");
        } catch (io.jsonwebtoken.security.SignatureException exception) {
            log.debug("JWT signature validation fails");
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT Token");
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT Token");
        } catch (IllegalArgumentException e) {
            log.debug("JWT claims string is empty");
        } catch (Exception exception) {
            log.error("JWT validation fails", exception);
        }
        return false;
    }


    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getNickname(String token) {
        return parseClaims(token).get("nickname", String.class);
    }

    public long getTokenVersion(String token) {
        Object ver = parseClaims(token).get("ver");
        return ver == null ? 0L : ((Number) ver).longValue();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Authentication getAuthentication(String token) {
        Long memberId = getUserId(token);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        UserDetails userDetails = new CustomUserDetails(member.getId(), member.getNickname());
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}
