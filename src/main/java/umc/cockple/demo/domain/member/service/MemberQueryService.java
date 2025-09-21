package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.converter.MemberConverter;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberKeyword;
import umc.cockple.demo.domain.member.dto.GetAllAddressResponseDTO;
import umc.cockple.demo.domain.member.dto.GetMyProfileResponseDTO;
import umc.cockple.demo.domain.member.dto.GetNowAddressResponseDTO;
import umc.cockple.demo.domain.member.dto.GetProfileResponseDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Keyword;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static umc.cockple.demo.domain.member.converter.MemberConverter.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final ImageService imageService;

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

        // 엔티티 -> 값 타입으로 변환
        List<Keyword> keywords = member.getKeywords().stream()
                .map(MemberKeyword::getKeyword)
                .toList();


        return toGetMyProfileResponseDTO(profileDto, memberAddr, exerciseCnt, keywords);
    }


    public GetProfileResponseDTO getProfile(Long memberId) {
        // 회원 조회
        Member member = findByMemberId(memberId);

        // 프로필 사진 null-safety
        String imgUrl = null;
        if (member.getProfileImg() != null) {
            imgUrl = imageService.getUrlFromKey(member.getProfileImg().getImgKey());
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

    public List<GetAllAddressResponseDTO> getAllAddress(Long memberId) {
        // 해당 회원 조회
        Member member = findByMemberId(memberId);

        // 주소가 존재하지 않을 시 예외처리
        if (member.getAddresses().isEmpty()) {
            throw new MemberException(MemberErrorCode.MEMBER_ADDRESS_MINIMUM_REQUIRED);
        }

        // 모든 주소 -> DTO convert
        return member.getAddresses().stream()
                .sorted((a, b) -> {
                    if (a.getIsMain() && !b.getIsMain()) return -1;  // 대표주소 먼저
                    if (!a.getIsMain() && b.getIsMain()) return 1;
                    return a.getId().compareTo(b.getId());  // 나머지는 id 순
                })
                .map(MemberConverter::toGetAllAddressResponseDTO)
                .toList();
    }

    public MemberConnectionInfo getMemberConnectionInfo(Long memberId) {
        Member member = findByMemberId(memberId);

        return MemberConnectionInfo.builder()
                .memberId(memberId)
                .memberName(member.getMemberName())
                .build();
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
