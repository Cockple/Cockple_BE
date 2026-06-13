package umc.cockple.demo.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.auth.RefreshTokenRepository;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.jwt.domain.TokenRefreshResponse;
import umc.cockple.demo.global.oauth2.domain.KakaoClient;
import umc.cockple.demo.global.oauth2.domain.info.KakaoClientInfo;

import java.util.Optional;

import static umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO.*;

@Service
@RequiredArgsConstructor
public class KakaoOauthService {

    private final KakaoClient kakaoClient;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
                        .isActive(MemberStatus.ACTIVE)
                        .build())
        );

        if (member.getIsActive() == MemberStatus.INACTIVE) {
            member.rejoin();
            newMember = true;
        }

        // 4. jwt 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname());

        // 5. refresh는 redis에 저장
        refreshTokenRepository.save(refreshToken, member.getId());

        // 온보딩(상세정보 입력) 필요 여부 - 신규/미완성/재가입 회원 모두 true
        boolean needsOnboarding = !member.isProfileCompleted();

        // jwt개발할 때 넣기
        return new KakaoLoginResponseDTO(accessToken, refreshToken, member.getId(), member.getNickname(), newMember, needsOnboarding);
    }

    public void unlinkAccess(Member member) {
        if (member.getSocialId() != null) {
            try {
                kakaoClient.unlinkByAdmin(member.getSocialId());
            } catch (Exception e) {
                throw new MemberException(MemberErrorCode.OAUTH_UNLINK_FAIL);
            }
        }

    }

    public KakaoLoginResponseDTO createDevToken() {
        // 특정 member 가져오기
        Member member = memberRepository.findById(1L)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // accessToken: 2주 만료
        String accessToken = jwtTokenProvider.createDevToken(member.getId(), member.getNickname());

        // refreshToken: 기본 만료
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname());

        // refreshToken Redis에 저장
        refreshTokenRepository.save(refreshToken, member.getId());

        return KakaoLoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .isNewMember(false)
                .needsOnboarding(!member.isProfileCompleted())
                .build()
                ;
    }

    public KakaoLoginResponseDTO createOtherDevToken() {
        // 특정 member 가져오기
        Member member = memberRepository.findById(2L)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // accessToken: 2주 만료
        String accessToken = jwtTokenProvider.createDevToken(member.getId(), member.getNickname());

        // refreshToken: 기본 만료
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname());

        // refreshToken Redis에 저장
        refreshTokenRepository.save(refreshToken, member.getId());

        return KakaoLoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .isNewMember(false)
                .needsOnboarding(!member.isProfileCompleted())
                .build()
                ;
    }

    public TokenRefreshResponse validateMember(String refreshToken) {
        // Redis에서 memberId 조회 및 삭제 (GETDEL - 원자적 처리로 동시 요청 시 중복 발급 방지)
        Long memberId = refreshTokenRepository.findAndDeleteByToken(refreshToken)
                .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 탈퇴한 회원 차단
        if (member.getIsActive() == MemberStatus.INACTIVE) {
            throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 액세스 토큰 재발급
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getNickname());

        // 새 리프레시 토큰 발급 및 Redis 저장
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname());
        refreshTokenRepository.save(newRefreshToken, member.getId());

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }
}
