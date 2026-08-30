package umc.cockple.demo.domain.member.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.member.service.MemberQueryService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ContestFixture;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;

import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

// 프로필 조회 최적화(대회/모임/운동 컬렉션 전량 로딩 → 집계·COUNT 쿼리) 회귀 방지 테스트
@DisplayName("프로필 조회 쿼리 카운트 테스트")
class MemberProfileQueryCountTest extends IntegrationTestBase {

    /**
     * getProfile 기대 쿼리 수 (대회/모임 개수와 무관하게 고정)
     * 1) 회원  2) 메달 집계  3) 가입 모임 수 COUNT
     * (프로필 이미지가 없는 회원이라 이미지 조회 쿼리는 발생하지 않는다)
     */
    private static final int GET_PROFILE_QUERY_COUNT = 3;

    /**
     * getMyProfile 기대 쿼리 수 (대회/모임/운동 개수와 무관하게 고정)
     * getProfile(3) + 대표주소 + 참여 운동 수 COUNT + 키워드
     */
    private static final int GET_MY_PROFILE_QUERY_COUNT = 6;

    @Autowired MemberQueryService memberQueryService;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired ContestRepository contestRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;

    @PersistenceContext EntityManager em;

    @AfterEach
    void tearDown() {
        memberExerciseRepository.deleteAll();
        contestRepository.deleteAll();
        memberPartyRepository.deleteAll();
        exerciseRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("getProfile - 대회/모임 개수와 무관하게 고정된 쿼리 수만 실행한다")
    void getProfile_hasNoNPlusOne() {
        Member small = seedMember(3001L, 2);
        Member large = seedMember(3002L, 10);

        assertQueryCount(em, GET_PROFILE_QUERY_COUNT, () ->
                memberQueryService.getProfile(small.getId()));
        assertQueryCount(em, GET_PROFILE_QUERY_COUNT, () ->
                memberQueryService.getProfile(large.getId()));
    }

    @Test
    @DisplayName("getMyProfile - 대회/모임/운동 개수와 무관하게 고정된 쿼리 수만 실행한다")
    void getMyProfile_hasNoNPlusOne() {
        Member small = seedMember(3003L, 2);
        Member large = seedMember(3004L, 10);

        assertQueryCount(em, GET_MY_PROFILE_QUERY_COUNT, () ->
                memberQueryService.getMyProfile(small.getId()));
        assertQueryCount(em, GET_MY_PROFILE_QUERY_COUNT, () ->
                memberQueryService.getMyProfile(large.getId()));
    }

    // === 시딩 ===

    // 회원 + 대표주소 1개 + 대회 n개 + (모임 n개 가입) + (운동 n개 참여)
    private Member seedMember(long socialId, int n) {
        Member member = memberRepository.save(
                MemberFixture.createMember("회원" + socialId, Gender.MALE, Level.A, socialId));

        memberAddrRepository.save(
                MemberAddrFixture.createAddr(member, "역삼동", "서울특별시 강남구 테헤란로 1", true));

        for (int i = 0; i < n; i++) {
            contestRepository.save(ContestFixture.createContest(member, "대회" + socialId + "_" + i, MedalType.GOLD));

            PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("경기도", "모임" + socialId + "_" + i));
            Party party = partyRepository.save(PartyFixture.createParty("모임" + socialId + "_" + i, member.getId(), addr));
            memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.PARTY_MEMBER));

            var exercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(i + 1)));
            memberExerciseRepository.save(MemberFixture.createMemberExercise(member, exercise));
        }
        return member;
    }
}
