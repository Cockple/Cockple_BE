package umc.cockple.demo.support;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 특정 동작이 실행하는 SQL 쿼리 수를 단언하는 테스트 헬퍼.
 *
 * Hibernate의 prepared statement 카운터를 이용해 "이 호출은 SQL이 정확히 N번 나가야 한다"를 박제한다.
 * N+1이 발생하면 카운트가 늘어나 테스트가 실패하므로, N+1 회귀를 CI에서 자동으로 잡아낸다.
 *
 */
public final class QueryCountAssert {

    private QueryCountAssert() {
    }

    public static void assertQueryCount(EntityManager em, long expected, Runnable action) {
        Statistics statistics = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();

        statistics.clear();
        action.run();
        long actual = statistics.getPrepareStatementCount();

        assertThat(actual)
                .as("실행된 SQL 쿼리 수가 기대치와 다릅니다 (N+1 가능성)")
                .isEqualTo(expected);
    }
}
