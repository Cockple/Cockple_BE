package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.member.converter.MemberConverter;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.dto.GetMyProfileResponseDTO;
import umc.cockple.demo.domain.member.dto.GetNowAddressResponseDTO;
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

    /*
    * 프로필 관련 조회 메서드
    * */
    public GetMyProfileResponseDTO getMyProfile(Long memberId) {
        // 회원 조회
        Member member = findByMemberId(memberId);

        // 프로필 조회하기
        GetProfileResponseDTO profileDto = getProfile(memberId);

        // 대표 주소 추출
        MemberAddr memberAddr = findMainAddress(member);

        // 운동 개수 추출
        int exerciseCnt = member.getMemberExercises().size();

        return toGetMyProfileResponseDTO(profileDto, memberAddr, exerciseCnt);
    }


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

    /*
     * 주소 관련 조회 메서드
     * */

    public GetNowAddressResponseDTO getNowAddress(Long memberId) {
        // 해당 회원 조회
        Member member = findByMemberId(memberId);

        // 대표 주소 추출
        MemberAddr mainAddress = findMainAddress(member);

        return toGetNowAddressResponseDTO(mainAddress);
    }


    /*
     * private 메서드
     * */
    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private static MemberAddr findMainAddress(Member member) {
        return member.getAddresses().stream()
                .filter(MemberAddr::getIsMain)
                .findFirst()
                .orElseThrow(() -> new MemberException(MemberErrorCode.MAIN_ADDRESS_NULL));
    }

}
