package umc.cockple.demo.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.oauth2.domain.KakaoClient;
import umc.cockple.demo.global.oauth2.domain.info.KakaoClientInfo;

import java.util.Optional;

import static umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO.*;

@Service
@RequiredArgsConstructor
public class KakaoOauthService {

    private final KakaoClient kakaoClient;
    private final MemberRepository memberRepository;

    @Transactional
    public KakaoLoginResponseDTO signup(String code) {
        // 1. accessToken 발급
        String accessToken = kakaoClient.getAccessToken(code);

        // 2. 카카오에 사용자 정보 요청
        KakaoClientInfo info = kakaoClient.getClientInfo(accessToken);

        // 3. 기존 유저 여부 확인
        Optional<Member> optionalMember = memberRepository.findBySocialId(info.kakaoId());
        boolean newMember = optionalMember.isEmpty();

        Member member = optionalMember.orElseGet(() ->
                memberRepository.save(Member.builder()
                        .socialId(info.kakaoId())
                        .nickname(info.nickname())
                        .build())
        );

        // jwt개발할 때 넣기
        return new KakaoLoginResponseDTO(null, member.getId(), member.getNickname(), newMember);
    }

}
