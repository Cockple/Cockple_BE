package umc.cockple.demo.domain.bookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.ExerciseOrderType;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BookmarkQueryService {

    private final ExerciseBookmarkRepository exerciseBookmarkRepository;
    private final PartyBookmarkRepository partyBookmarkRepository;
    private final MemberRepository memberRepository;

//    public List<GetAllExerciseBookmarksResponseDTO> getAllExerciseBookmarks(Long memberId, ExerciseOrderType orderType) {
//        // 회원 조회하기
//        Member member = findByMemberId(memberId);
//
//        // 회원의 찜한 운동 가져오기
//        List<ExerciseBookmark> bookmarks = exerciseBookmarkRepository.findAllByMember(member);
//
//
//
//    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
