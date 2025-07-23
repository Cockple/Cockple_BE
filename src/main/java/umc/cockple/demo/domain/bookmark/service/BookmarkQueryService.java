package umc.cockple.demo.domain.bookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.converter.BookmarkConverter;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.ExerciseOrderType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BookmarkQueryService {

    private final ExerciseBookmarkRepository exerciseBookmarkRepository;
    private final PartyBookmarkRepository partyBookmarkRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final MemberRepository memberRepository;

    public List<GetAllExerciseBookmarksResponseDTO> getAllExerciseBookmarks(Long memberId, ExerciseOrderType orderType) {
        // 회원 조회하기
        Member member = findByMemberId(memberId);

        // 찜한 운동 가져오기
        List<ExerciseBookmark> bookmarks = exerciseBookmarkRepository.findAllByMember(member);

        // orderType에 따른 정렬
        Comparator<ExerciseBookmark> comparator = Comparator.comparing(ExerciseBookmark::getCreatedAt);
        if (orderType == ExerciseOrderType.LATEST) comparator = comparator.reversed();
        bookmarks.sort(comparator);

        // include 정보 미리 조회
        List<Long> partyIds = bookmarks.stream().map(b -> b.getExercise().getParty().getId()).toList();
        List<Long> exerciseIds = bookmarks.stream().map(b -> b.getExercise().getId()).toList();

        List<Long> myParties = memberPartyRepository.findAllPartyIdsByMemberAndPartyIds(memberId, partyIds);
        List<Long> myExercises = memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(memberId, exerciseIds);

        // bookmark -> dto 변환
        return bookmarks.stream()
                .map(bookmark -> {
                    boolean includeParty = myParties.contains(bookmark.getExercise().getParty().getId());
                    boolean includeExercise = myExercises.contains(bookmark.getExercise().getId());
                    return BookmarkConverter.exerciseBookmarkToDTO(bookmark, includeParty, includeExercise);
                })
                .toList();
    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
