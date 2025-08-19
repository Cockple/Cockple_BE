package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.member.domain.*;
import umc.cockple.demo.domain.member.dto.MemberDetailInfoRequestDTO;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.*;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.image.service.ImageService;

import java.util.List;

import static umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO.*;
import static umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository;
    private final MemberAddrRepository memberAddrRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final ImageService imageService;


    // ==================== 회원 관련 ===================

    public void memberDetailInfo(Long memberId, MemberDetailInfoRequestDTO requestDTO) {
        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 키워드 저장
        List<MemberKeyword> keywords = requestDTO.keywords().stream()
                .map(keyword -> {
                    MemberKeyword memberKeyword = MemberKeyword.builder()
                            .member(member)
                            .keyword(keyword)
                            .build();

                    member.getKeywords().add(memberKeyword);
                    return memberKeyword;

                })
                .toList();

        memberKeywordRepository.saveAll(keywords);

        if (requestDTO.imgKey() != null) {
            ProfileImg profile = ProfileImg.builder()
                    .member(member)
                    .imgKey(requestDTO.imgKey())
                    .build();

            member.updateMemberFirst(requestDTO, keywords, profile);

        } else {
            member.updateMemberFirst(requestDTO, keywords);
        }

    }


    public void withdrawMember(Long memberId) {
        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 탈퇴 가능여부 검증
        validateCanWithdraw(member);

        // 참여중인 운동, 모임에서 나가기
        memberExerciseRepository.deleteAllByMember(member);
        memberPartyRepository.deleteAllByMember(member);

        // 활성화 여부 해제, 리프레시 토큰 삭제
        member.withdraw();
    }


    // ==================== 프로필 관련 ===================

    public void updateProfile(UpdateProfileRequestDTO requestDto, Long memberId) {
        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 기존 키워드 삭제
        memberKeywordRepository.deleteAllByMember(member);

        // 받아온 키워드 반영
        List<MemberKeyword> keywords = requestDto.keywords().stream()
                .map(keyword -> {
                    MemberKeyword memberKeyword = MemberKeyword.builder()
                            .member(member)
                            .keyword(keyword)
                            .build();

                    member.getKeywords().add(memberKeyword);
                    return memberKeyword;

                })
                .toList();

        memberKeywordRepository.saveAll(keywords);

        log.info("===== 프로필 이미지 key값 확인 : " + requestDto.imgKey());

        // 이미지 -> 저장 후 url 받아오기
        String imgKey = requestDto.imgKey();

        // 받은 key가 null인지 확인
        if (!StringUtils.hasText(imgKey)) {
            member.updateMember(requestDto, keywords);
        } else {
            // 기존 이미지 존재시 이미지 새로 업로드
            if (member.getProfileImg() != null && StringUtils.hasText(member.getProfileImg().getImgKey())) {

                // 프로필 사진이 변경되었을 경우에만 이미지 url 변경 및 S3 사진 변경
                if (!member.getProfileImg().getImgKey().equals(imgKey)) {
                    imageService.delete(member.getProfileImg().getImgKey());
                    member.getProfileImg().updateProfile(imgKey);
                }

                // 회원 정보 수정하기 (프로필 사진 제외)
                member.updateMember(requestDto, keywords);

            } else {
                // 받아온 이미지로 profile객체 생성
                ProfileImg img = ProfileImg.builder()
                        .member(member)
                        .imgKey(imgKey)
                        .build();

                // 회원 정보 수정하기 (프로필 사진까지)
                member.updateMember(requestDto, keywords, img);

            }
        }

        //chatRoomMember의 displayName도 같이 업데이트
        List<ChatRoomMember> chatRoomMembers = chatRoomMemberRepository.findAllByMemberId(member.getId());
        chatRoomMembers.forEach(crm -> crm.updateDisplayName(requestDto.memberName()));

    }


    // ==================== 주소 관련 ===================

    public CreateMemberAddrResponseDTO addMemberNewAddr(CreateMemberAddrRequestDTO requestDto, Long memberId) {

        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 원하는 주소가 이미 존재하고 있는지 확인
        if (member.hasDuplicateAddr(requestDto)) {
            throw new MemberException(MemberErrorCode.DUPLICATE_ADDRESS);
        }

        // 주소 개수 5개 이상인지 확인
        if (member.getAddresses().size() >= 5) {
            throw new MemberException(MemberErrorCode.OVER_NUMBER_OF_ADDR);
        }

        // 기존 대표주소 해제
        member.getAddresses().stream()
                .filter(MemberAddr::getIsMain)
                .findFirst()
                .ifPresent(MemberAddr::notMainAddr)
        ;


        // 주소 생성
        MemberAddr memberAddr = MemberAddr.builder()
                .addr1(requestDto.addr1())
                .addr2(requestDto.addr2())
                .addr3(requestDto.addr3())
                .streetAddr(requestDto.streetAddr())
                .buildingName(requestDto.buildingName())
                .latitude(requestDto.latitude())
                .longitude(requestDto.longitude())
                .isMain(true)
                .member(member)
                .build();

        // 주소 등록
        MemberAddr newAddr = memberAddrRepository.save(memberAddr);
        member.getAddresses().add(newAddr);

        return new CreateMemberAddrResponseDTO(newAddr.getId());

    }

    public void updateMainAddr(Long memberId, Long addrId) {
        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 새 대표주소 찾기
        MemberAddr newMainAddr = findByAddrId(addrId);

        // 현재 대표주소 찾아서 false로 변경
        member.getAddresses().stream()
                .filter(MemberAddr::getIsMain)
                .findFirst()
                .ifPresent(MemberAddr::notMainAddr)
        ;

        // 새 대표주소 true로 설정
        newMainAddr.beMainAddr();
    }

    public void deleteMemberAddr(Long memberId, Long addrId) {
        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 지우려는 주소 조회
        MemberAddr addr = findByAddrId(addrId);

        // 지우려는 주소가 대표주소인지 확인 -> 대표주소면 지울 수 없음
        if (addr.getIsMain()) {
            throw new MemberException(MemberErrorCode.CANNOT_REMOVE_MAIN_ADDR);
        }

        // 주소가 1개 이하면 삭제 불가
        if (member.getAddresses().size() <= 1) {
            throw new MemberException(MemberErrorCode.MEMBER_ADDRESS_MINIMUM_REQUIRED);
        }

        // 주소 삭제
        memberAddrRepository.deleteById(addrId);
        member.getAddresses().remove(addr);

    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberAddr findByAddrId(Long memberAddrId) {
        return memberAddrRepository.findById(memberAddrId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.ADDRESS_NOT_FOUND));
    }

    private void validateCanWithdraw(Member member) {
        // 이미 탈퇴했을 경우 -> 탈퇴 불가
        if (member.getIsActive() == MemberStatus.INACTIVE) {
            throw new MemberException(MemberErrorCode.ALREADY_WITHDRAW);
        }

        // 활성화 된 모임의 모임장인 경우 -> 탈퇴 불가
        boolean isLeader = member.getMemberParties().stream()
                .anyMatch(memberParty ->
                        memberParty.isLeader()
                                && memberParty.getStatus() == MemberPartyStatus.ACTIVE
                );

        if (isLeader) {
            throw new MemberException(MemberErrorCode.MANAGER_CANNOT_LEAVE);
        }
    }
}
