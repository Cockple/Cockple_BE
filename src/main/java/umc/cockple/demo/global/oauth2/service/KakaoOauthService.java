package umc.cockple.demo.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.auth.RefreshTokenRepository;
import umc.cockple.demo.global.auth.TokenVersionRepository;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.jwt.domain.TokenRefreshResponse;
import umc.cockple.demo.global.oauth2.domain.KakaoClient;
import umc.cockple.demo.global.oauth2.domain.info.KakaoClientInfo;

import java.util.Optional;

import static umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOauthService {

    private final KakaoClient kakaoClient;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenVersionRepository tokenVersionRepository;

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

        // 4. jwt 발급 (현재 tokenVersion 주입)
        long tokenVersion = tokenVersionRepository.getVersion(member.getId());
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getNickname(), tokenVersion);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname(), tokenVersion);

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
        long tokenVersion = tokenVersionRepository.getVersion(member.getId());
        String accessToken = jwtTokenProvider.createDevToken(member.getId(), member.getNickname(), tokenVersion);

        // refreshToken: 기본 만료
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname(), tokenVersion);

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
        long tokenVersion = tokenVersionRepository.getVersion(member.getId());
        String accessToken = jwtTokenProvider.createDevToken(member.getId(), member.getNickname(), tokenVersion);

        // refreshToken: 기본 만료
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname(), tokenVersion);

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
        Optional<Long> memberIdOpt = refreshTokenRepository.consumeAndMark(refreshToken);

        // 활성 저장소에 없는 경우 - 재사용(탈취) 여부를 판별해 대응한 뒤 거부
        if (memberIdOpt.isEmpty()) {
            detectAndHandleReuse(refreshToken);
            throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = memberIdOpt.get();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 탈퇴한 회원 차단
        if (member.getIsActive() == MemberStatus.INACTIVE) {
            throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 버전 검증 - 강제 무효화(탈퇴/탈취 대응 등)된 리프레시 토큰 차단
        long tokenVersion = tokenVersionRepository.getVersion(member.getId());
        if (jwtTokenProvider.getTokenVersion(refreshToken) != tokenVersion) {
            throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 액세스 토큰 재발급 (현재 tokenVersion 주입)
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getNickname(), tokenVersion);

        // 새 리프레시 토큰 발급 및 Redis 저장
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getNickname(), tokenVersion);
        refreshTokenRepository.save(newRefreshToken, member.getId());

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    private void detectAndHandleReuse(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return;
        }
        // refresh 용도가 아닌 토큰(access를 재발급에 오용)은 탈취가 아니므로 무효화하지 않고 단순 거부
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            return;
        }
        // grace window 이내 정상 소비 이력 존재 → 동시 재발급 경쟁/재시도로 판단, 무효화 x
        if (refreshTokenRepository.isRecentlyConsumed(refreshToken)) {
            return;
        }
        // 재사용(탈취) 확정
        Long memberId = jwtTokenProvider.getUserId(refreshToken);
        long newVersion = tokenVersionRepository.increment(memberId);
        log.warn("리프레시 토큰 재사용 감지 - 회원 {} 의 모든 토큰을 무효화합니다. (tokenVersion={})", memberId, newVersion);
    }
}
