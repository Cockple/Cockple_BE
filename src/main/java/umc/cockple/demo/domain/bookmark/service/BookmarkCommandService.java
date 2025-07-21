package umc.cockple.demo.domain.bookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.exception.BookmarkErrorCode;
import umc.cockple.demo.domain.bookmark.exception.BookmarkException;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.PartyOrderType;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BookmarkCommandService {

    private final PartyBookmarkRepository partyBookmarkRepository;
    private final ExerciseBookmarkRepository exerciseBookmarkRepository;
    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final ExerciseRepository exerciseRepository;

    public Long partyBookmark(Long memberId, Long partyId) {
        // 회원 조회하기
        Member member = findByMemberId(memberId);

        // 찜하고 싶은 모임 조회
        Party party = findByPartyId(partyId);

        // 찜 여부 확인
        if (partyBookmarkRepository.existsByMemberAndParty(member, party)) {
            throw new BookmarkException(BookmarkErrorCode.ALREADY_BOOKMARK);
        }

        PartyBookmark bookmark = PartyBookmark.builder()
                .party(party)
                .member(member)
                .orderType(PartyOrderType.LATEST) // 없앨수도
                .build()
        ;

        // 찜하기
        partyBookmarkRepository.save(bookmark);
        return bookmark.getId();
    }


    public void releasePartyBookmark(Long memberId, Long partyId) {
        // 회원 조회하기
        Member member = findByMemberId(memberId);

        // 찜 해제 싶은 모임 조회
        Party party = findByPartyId(partyId);

        // 모임 찜 찾기
        PartyBookmark bookmark = findByMemberAndParty(member, party);

        // 찜 지우기
        partyBookmarkRepository.delete(bookmark);
        member.getPartyBookmarks().remove(bookmark);
    }


    private Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private Party findByPartyId(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
    }

    private PartyBookmark findByMemberAndParty(Member member, Party party) {
        return partyBookmarkRepository.findByMemberAndParty(member, party)
                .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK));
    }


}
