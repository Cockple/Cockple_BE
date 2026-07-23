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
 * 1차 캐시를 비워 {@code action} 안의 조회가 실제 DB를 때리게 한다. (캐시 hit으로 인한 과소 집계 방지 → 운영의 콜드 캐시 상황에 맞춤)
 *
 * 측정 전 clear()로 영속성 컨텍스트가 비워지므로, 이 메서드 호출 이후 픽스처 엔티티는 detach 상태가 된다. 호출 뒤 지연 로딩 접근 시
 * LazyInitializationException이 날 수 있으니, 필요한 값은 호출 전에 읽거나 재조회한다.
 */
public final class QueryCountAssert {

    private QueryCountAssert() {
    }

    public static void assertQueryCount(EntityManager em, long expected, Runnable action) {
        Statistics statistics = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();

        // 측정 전: 1차 캐시를 비워 action의 조회가 실제 DB를 때리도록 한다.
        if (em.isJoinedToTransaction()) {
            em.clear();
        }

        statistics.clear();
        action.run();

        // 측정 후: action이 유발한 쓰기 지연 SQL을 카운트 읽기 전에 강제로 내보낸다.
        if (em.isJoinedToTransaction()) {
            em.flush();
        }

        long actual = statistics.getPrepareStatementCount();

        assertThat(actual)
                .as("실행된 SQL 쿼리 수가 기대치와 다릅니다 (N+1 가능성)")
                .isEqualTo(expected);
    }

    public static void assertEntityLoadCount(EntityManager em, long expected, Runnable action) {
        Statistics statistics = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();

        if (em.isJoinedToTransaction()) {
            em.clear();
        }

        statistics.clear();
        action.run();

        if (em.isJoinedToTransaction()) {
            em.flush();
        }

        long actual = statistics.getEntityLoadCount();

        assertThat(actual)
                .as("DB에서 로딩된 엔티티 수가 기대치와 다릅니다 (불필요한 전량 로딩 가능성)")
                .isEqualTo(expected);
    }
}
