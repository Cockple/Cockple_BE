package umc.cockple.demo.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.jwt.domain.TokenRefreshResponse;
import umc.cockple.demo.global.oauth2.domain.KakaoClient;
import umc.cockple.demo.global.oauth2.domain.info.KakaoClientInfo;

import java.time.Duration;
import java.util.Optional;

import static umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO.*;

@Service
@RequiredArgsConstructor
public class KakaoOauthService {

    private final KakaoClient kakaoClient;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public KakaoLoginResponseDTO signup(String code) {
        // 1. accessToken 발급
        String kakaoAccessToken = kakaoClient.getAccessToken(code);

        // 2. 카카오에 사용자 정보 요청
        KakaoClientInfo info = kakaoClient.getClientInfo(kakaoAccessToken);

        // 3. 기존 유저 여부 확인
        Optional<Member> optionalMember = memberRepository.findBySocialId(info.kakaoId());
        boolean newMember = optionalMember.isEmpty();

        Member member = optionalMember.orElseGet(() ->
                memberRepository.save(Member.builder()
                        .socialId(info.kakaoId())
                        .nickname(info.nickname())
                        .build())
        );

        // 4. jwt 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname());

        // 5. refresh는 db에 저장
        member.setRefreshToken(refreshToken);

        // jwt개발할 때 넣기
        return new KakaoLoginResponseDTO(accessToken, refreshToken, member.getId(), member.getNickname(), newMember);
    }

    public TokenRefreshResponse validateMember(String refreshToken) {
        Member member = memberRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN));

        // 액세스 토큰 재발급
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getNickname());

        // 리프레시 토큰 만료가 3일 이하로 남은 경우 갱신 (sliding session)
        long expired = 3 * 24 * 60 * 60 * 1000L;
        if (jwtTokenProvider.isTokenExpiringSoon(refreshToken, expired)) {
            String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname());
            member.setRefreshToken(newRefreshToken);

            return new TokenRefreshResponse(newAccessToken, newRefreshToken);
        }

        return new TokenRefreshResponse(newAccessToken, refreshToken);
    }
}
