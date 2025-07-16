package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberKeyword;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberKeywordRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.s3.ImageService;

import java.util.List;

import static umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository;
    private final MemberAddrRepository memberAddrRepository;

    private final ImageService imageService;

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

        // 이미지 -> 저장 후 url 받아오기
        String imgUrl = requestDto.imgUrl();

        // 기존 이미지 존재시 이미지 새로 업로드
        if (member.getProfileImg() != null) {

            // 프로필 사진이 변경되었을 경우에만 이미지 url 변경 및 S3 사진 변경
            if (!member.getProfileImg().getImgUrl().equals(imgUrl)) {
                imageService.delete(member.getProfileImg().getImgUrl());
                member.getProfileImg().updateProfile(imgUrl);
            }

            // 회원 정보 수정하기 (프로필 사진 제외)
            member.updateMember(requestDto, keywords);

        } else {
            // 받아온 이미지로 profile객체 생성
            ProfileImg img = ProfileImg.builder()
                    .member(member)
                    .imgUrl(imgUrl)
                    .build()
                    ;

            // 회원 정보 수정하기 (프로필 사진까지)
            member.updateMember(requestDto, keywords, img);
        }
    }

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

        // 주소 생성
        MemberAddr memberAddr = MemberAddr.builder()
                .addr1(requestDto.addr1())
                .addr2(requestDto.addr2())
                .addr3(requestDto.addr3())
                .streetAddr(requestDto.streetAddr())
                .buildingName(requestDto.buildingName())
                .latitude(requestDto.latitude())
                .longitude(requestDto.longitude())
                .isMain(false)
                .member(member)
                .build()
        ;

        // 주소 등록
        MemberAddr newAddr = memberAddrRepository.save(memberAddr);
        member.getAddresses().add(newAddr);

        // 기존 대표주소 != 현재 등록하려는 대표주소 -> 대표주소 변경
        if (!requestDto.nowMainAddrId().equals(requestDto.prevMainAddrId())) {

            MemberAddr newMainAddr = findByAddrId(requestDto.nowMainAddrId());
            MemberAddr prevMainAddr = findByAddrId(requestDto.prevMainAddrId());

            // 새 대표주소 true처리, 이전 대표주소 false처리
            newMainAddr.beMainAddr();
            prevMainAddr.notMainAddr();
        }

        return new CreateMemberAddrResponseDTO(newAddr.getId());

    }

    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberAddr findByAddrId(Long memberAddrId) {
        return memberAddrRepository.findById(memberAddrId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.ADDRESS_NOT_FOUND));
    }

}
