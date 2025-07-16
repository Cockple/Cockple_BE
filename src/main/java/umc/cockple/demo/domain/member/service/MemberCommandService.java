package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberKeyword;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.dto.request.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberKeywordRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.s3.ImageService;

import java.util.List;

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



    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

}
