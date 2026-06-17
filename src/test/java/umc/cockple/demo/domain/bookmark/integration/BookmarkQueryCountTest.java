package umc.cockple.demo.domain.bookmark.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.enums.BookmarkedExerciseOrderType;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.bookmark.service.BookmarkQueryService;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.enums.PartyOrderType;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;

import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

/**
 * 찜 목록 조회의 N+1 회귀 방지 테스트.
 *
 * <p>같은 기대 쿼리 수를 서로 다른 찜 개수(N=2, N=8)에 대해 단언한다.
 * <ul>
 *   <li>두 N에서 쿼리 수가 같음 → 행 수에 비례하는 N+1이 없음(스케일 불변성)</li>
 *   <li>쿼리 수가 고정 상수 → fetch join 최적화가 유지됨. 누군가 fetch join을 제거하면
 *       (예: party의 역방향 @OneToOne partyImg/chatRoom) 쿼리 수가 늘어 테스트가 깨진다.</li>
 * </ul>
 */
@DisplayName("북마크 찜 목록 N+1 회귀 테스트")
class BookmarkQueryCountTest extends IntegrationTestBase {

    /**
     * 찜한 운동 목록 조회의 기대 쿼리 수.
     * 1) 회원 조회  2) 찜+운동+party+exerciseAddr+partyImg+chatRoom+levels fetch join
     * 3) 가입 모임 IN  4) 참여 운동 IN  5) 참여자 수 count  6) 게스트 수 count
     */
    private static final int EXERCISE_BOOKMARK_QUERY_COUNT = 6;

    /**
     * 찜한 모임 목록 조회의 기대 쿼리 수.
     * 1) 회원 조회  2) 찜+party+partyAddr+partyImg+chatRoom+exercises fetch join  3) levels 배치 로딩
     */
    private static final int PARTY_BOOKMARK_QUERY_COUNT = 3;

    @Autowired BookmarkQueryService bookmarkQueryService;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Autowired PartyBookmarkRepository partyBookmarkRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;

    @PersistenceContext EntityManager em;

    @AfterEach
    void tearDown() {
        exerciseBookmarkRepository.deleteAll();
        partyBookmarkRepository.deleteAll();
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("찜한 운동 목록 - 찜 개수와 무관하게 고정된 쿼리 수만 실행한다")
    void getAllExerciseBookmarks_hasNoNPlusOne() {
        Member small = createMember(7001L);
        Member large = createMember(7002L);
        for (int i = 0; i < 2; i++) seedExerciseBookmark(small, i);
        for (int i = 0; i < 8; i++) seedExerciseBookmark(large, i);

        assertQueryCount(em, EXERCISE_BOOKMARK_QUERY_COUNT, () ->
                bookmarkQueryService.getAllExerciseBookmarks(small.getId(), BookmarkedExerciseOrderType.LATEST));
        assertQueryCount(em, EXERCISE_BOOKMARK_QUERY_COUNT, () ->
                bookmarkQueryService.getAllExerciseBookmarks(large.getId(), BookmarkedExerciseOrderType.LATEST));
    }

    @Test
    @DisplayName("찜한 모임 목록 - 찜 개수와 무관하게 고정된 쿼리 수만 실행한다")
    void getAllPartyBookmarks_hasNoNPlusOne() {
        Member small = createMember(7003L);
        Member large = createMember(7004L);
        for (int i = 0; i < 2; i++) seedPartyBookmark(small, i);
        for (int i = 0; i < 8; i++) seedPartyBookmark(large, i);

        assertQueryCount(em, PARTY_BOOKMARK_QUERY_COUNT, () ->
                bookmarkQueryService.getAllPartyBookmarks(small.getId(), PartyOrderType.LATEST));
        assertQueryCount(em, PARTY_BOOKMARK_QUERY_COUNT, () ->
                bookmarkQueryService.getAllPartyBookmarks(large.getId(), PartyOrderType.LATEST));
    }

    // === 시딩 (BookmarkIntegrationTest 패턴 미러링) ===

    private Member createMember(long socialId) {
        return memberRepository.save(
                MemberFixture.createMember("회원" + socialId, Gender.MALE, Level.A, socialId));
    }

    private void seedExerciseBookmark(Member member, int idx) {
        Party party = newParty(member, "운동모임" + member.getId() + "_" + idx);
        memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.PARTY_MANAGER));
        var exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(idx + 1)));
        memberExerciseRepository.save(MemberFixture.createMemberExercise(member, exercise));
        exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                .member(member).exercise(exercise).build());
    }

    private void seedPartyBookmark(Member member, int idx) {
        Party party = newParty(member, "찜모임" + member.getId() + "_" + idx);
        exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(idx + 1)));
        partyBookmarkRepository.save(PartyBookmark.builder()
                .party(party).member(member).orderType(PartyOrderType.LATEST).build());
    }

    private Party newParty(Member member, String name) {
        // PartyAddr는 (addr1, addr2) UNIQUE 제약이 있어 addr2를 유니크한 name으로 만든다.
        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("경기도", name));
        return partyRepository.save(PartyFixture.createParty(name, member.getId(), addr));
    }
}
