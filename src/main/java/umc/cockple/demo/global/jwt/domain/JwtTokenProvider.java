package umc.cockple.demo.global.jwt.domain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.global.jwt.properties.JwtProperties;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private Key key;
    private final JwtProperties jwtProperties;

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
}
