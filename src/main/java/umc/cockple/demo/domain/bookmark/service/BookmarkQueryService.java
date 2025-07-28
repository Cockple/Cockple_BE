package umc.cockple.demo.domain.bookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.converter.BookmarkConverter;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.bookmark.dto.GetAllPartyBookmarkResponseDTO;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.ActivityTime;
import umc.cockple.demo.domain.bookmark.enums.BookmarkedExerciseOrderType;
import umc.cockple.demo.global.enums.PartyOrderType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    public List<GetAllExerciseBookmarksResponseDTO> getAllExerciseBookmarks(Long memberId, BookmarkedExerciseOrderType orderType) {
        // 회원 조회하기
        Member member = findByMemberId(memberId);

        // 찜한 운동 가져오기
        List<ExerciseBookmark> bookmarks = exerciseBookmarkRepository.findAllByMember(member);

        // orderType에 따른 정렬
        Comparator<ExerciseBookmark> comparator = Comparator.comparing(ExerciseBookmark::getCreatedAt);
        if (orderType == BookmarkedExerciseOrderType.LATEST) comparator = comparator.reversed();
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


    public List<GetAllPartyBookmarkResponseDTO> getAllPartyBookmarks(Long memberId, PartyOrderType orderType) {
        // 회원 조회하기
        Member member = findByMemberId(memberId);

        // 찜한 모임 가져오기
        List<PartyBookmark> bookmarks = partyBookmarkRepository.findAllByMemberWithParty(member);

        // orderType에 따른 정렬
        Comparator<PartyBookmark> comparator = switch (orderType) {
            case EXERCISE_COUNT ->
                    Comparator.comparingInt((PartyBookmark b) -> b.getParty().getExerciseCount()).reversed();
            case LATEST ->
                    Comparator.comparing(PartyBookmark::getCreatedAt).reversed();
            default ->
                    Comparator.comparing(PartyBookmark::getCreatedAt);
        };

        bookmarks.sort(comparator);

        return bookmarks.stream()
                 .map(bookmark -> {
                     // 가장 가까운 시일에 진행하는 운동 찾기
                     Exercise exercise = latestExercise(bookmark.getParty()).orElse(null);
                     ActivityTime activityTime = makeActiveTime(exercise);
                     return BookmarkConverter.partyBookmarkToDTO(bookmark, exercise, activityTime);
                 })
                .toList();
    }

    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private Optional<Exercise> latestExercise(Party party) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        return party.getExercises().stream()
                .filter(exercise ->
                        exercise.getDate().isAfter(today) ||
                                (exercise.getDate().isEqual(today) && exercise.getStartTime().isAfter(nowTime))
                )
                .min(Comparator.comparing(Exercise::getDate)
                        .thenComparing(Exercise::getStartTime));
    }

    private ActivityTime makeActiveTime(Exercise exercise) {
        if (exercise == null) return null;
        LocalTime time = exercise.getStartTime();

        if (time.isBefore(LocalTime.of(12, 0))) return ActivityTime.MORNING;
        return ActivityTime.AFTERNOON;
    }


}
