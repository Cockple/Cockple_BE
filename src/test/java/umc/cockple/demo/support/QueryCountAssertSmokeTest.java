package umc.cockple.demo.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

/**
 * {@link QueryCountAssert} 헬퍼가 SQL 쿼리 수를 올바르게 세는지 검증하는 스모크 테스트.
 * (인프라 자체의 동작 보증용 — 도메인별 N+1 테스트는 각 도메인 PR에서 추가)
 */
@DisplayName("QueryCountAssert 인프라 스모크 테스트")
class QueryCountAssertSmokeTest extends IntegrationTestBase {

    @Autowired
    MemberRepository memberRepository;

    @PersistenceContext
    EntityManager em;

    private Long savedMemberId;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 단건 조회(findById)는 정확히 1개의 SQL을 실행한다")
    void findById_executesExactlyOneQuery() {
        // given - 별도 트랜잭션에서 커밋 (이후 조회 시 1차 캐시에 남지 않음)
        Member saved = memberRepository.save(
                MemberFixture.createMember("스모크 유저", Gender.MALE, Level.A, 9001L));
        savedMemberId = saved.getId();

        // when & then - findById 한 번 = SELECT 한 번
        assertQueryCount(em, 1, () -> memberRepository.findById(savedMemberId));
    }
}
