package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberKeyword;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.dto.MemberRequestDto;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberKeywordRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.s3.ImageService;

import java.util.List;

import static umc.cockple.demo.domain.member.dto.MemberRequestDto.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository;

    private final ImageService imageService;

    public void updateProfile(UpdateProfileRequestDto requestDto, MultipartFile file, Long memberId) {
        // 회원 찾기
        Member member = findByMemberId(memberId);

        // 이미지 -> 저장 후 url 받아오기
        String imgUrl = imageService.uploadImage(file);

        // 받아온 이미지로 profile객체 생성
        ProfileImg img = ProfileImg.builder()
                .member(member)
                .imgUrl(imgUrl)
                .build()
        ;

        // 기존 이미지 존재 시 삭제 (S3 연동 후 바뀌는 메서드에 따라 파라미터 변경 가능)
        if (member.getProfileImg() != null) {
            imageService.delete(member.getProfileImg().getImgUrl());
        }

        // 기존 키워드 삭제
        memberKeywordRepository.deleteAllByMember(member);

        // 받아온 키워드 반영
        List<MemberKeyword> keywords = requestDto.getKeywords().stream()
                .map(keyword -> MemberKeyword.builder()
                        .member(member)
                        .keyword(keyword)
                        .build())
                .toList()
        ;

        memberKeywordRepository.saveAll(keywords);

        // 회원 정보 수정하기
        member.updateMember(requestDto, keywords, img);
    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
