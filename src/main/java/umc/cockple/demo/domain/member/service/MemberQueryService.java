package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.member.converter.MemberConverter;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.dto.GetProfileResponseDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.MedalType;

import java.util.Map;
import java.util.stream.Collectors;

import static umc.cockple.demo.domain.member.converter.MemberConverter.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public GetProfileResponseDTO getProfile(Long memberId) {
        // 회원 조회
        Member member = findByMemberId(memberId);

        // 프로필 사진 null-safety
        String imgUrl = null;
        if (member.getProfileImg() != null) {
            imgUrl = member.getProfileImg().getImgUrl();
        }

        // 각 메달 개수 카운트
        Map<MedalType, Long> counts = member.getContests().stream()
                .collect(Collectors.groupingBy(Contest::getMedalType, Collectors.counting()));

        int goldMedal = counts.getOrDefault(MedalType.GOLD, 0L).intValue();
        int silverMedal = counts.getOrDefault(MedalType.SILVER, 0L).intValue();
        int bronzeMedal = counts.getOrDefault(MedalType.BRONZE, 0L).intValue();

        return memberToGetProfileResponseDTO(member, goldMedal, silverMedal, bronzeMedal, imgUrl);
    }

    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
