package umc.cockple.demo.domain.contest.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.domain.contest.dto.ContestMedalSummaryDTO;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.contest.service.ContestQueryService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ContestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

// 메달 개수 조회 쿼리 최적화(GOLD/SILVER/BRONZE 3회 → 조건부 집계 1회) 회귀 방지 테스트
@DisplayName("메달 개수 조회 쿼리 카운트 테스트")
class ContestMedalSummaryQueryCountTest extends IntegrationTestBase {

    /**
     * 메달 개수 조회의 기대 쿼리 수
     * 종류별로 나누지 않고 조건부 집계로 한 번에 세므로, 메달 개수와 무관하게 1이어야 한다.
     */
    private static final int MEDAL_SUMMARY_QUERY_COUNT = 1;

    @Autowired ContestQueryService contestQueryService;
    @Autowired MemberRepository memberRepository;
    @Autowired ContestRepository contestRepository;

    @PersistenceContext EntityManager em;

    @AfterEach
    void tearDown() {
        contestRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("메달 개수와 무관하게 항상 1개의 쿼리만 실행한다")
    void getMyMedalSummary_runsSingleQuery() {
        Member member = createMember(8001L);
        // GOLD 2, SILVER 1, BRONZE 1, NONE 1
        seed(member, MedalType.GOLD, MedalType.GOLD, MedalType.SILVER, MedalType.BRONZE, MedalType.NONE);

        assertQueryCount(em, MEDAL_SUMMARY_QUERY_COUNT, () ->
                contestQueryService.getMyMedalSummary(member.getId()));
    }

    @Test
    @DisplayName("조건부 집계 결과가 종류별 개수·합계와 일치한다")
    void getMyMedalSummary_returnsCorrectCounts() {
        Member member = createMember(8002L);
        seed(member, MedalType.GOLD, MedalType.GOLD, MedalType.SILVER, MedalType.BRONZE, MedalType.NONE);

        ContestMedalSummaryDTO.Response response = contestQueryService.getMyMedalSummary(member.getId());

        assertThat(response.goldCount()).isEqualTo(2);
        assertThat(response.silverCount()).isEqualTo(1);
        assertThat(response.bronzeCount()).isEqualTo(1);
        assertThat(response.myMedalTotal()).isEqualTo(4); // NONE은 합계에 미포함
    }

    @Test
    @DisplayName("메달이 없으면 모두 0을 반환한다")
    void getMyMedalSummary_noMedals() {
        Member member = createMember(8003L);

        ContestMedalSummaryDTO.Response response = contestQueryService.getMyMedalSummary(member.getId());

        assertThat(response.goldCount()).isZero();
        assertThat(response.silverCount()).isZero();
        assertThat(response.bronzeCount()).isZero();
        assertThat(response.myMedalTotal()).isZero();
    }

    // === 시딩 ===

    private Member createMember(long socialId) {
        return memberRepository.save(
                MemberFixture.createMember("회원" + socialId, Gender.MALE, Level.A, socialId));
    }

    private void seed(Member member, MedalType... medalTypes) {
        int idx = 0;
        for (MedalType medalType : medalTypes) {
            contestRepository.save(
                    ContestFixture.createContest(member, "대회" + member.getId() + "_" + idx++, medalType));
        }
    }
}
