package umc.cockple.demo.support;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 특정 동작이 실행하는 SQL 쿼리 수를 단언하는 테스트 헬퍼.
 *
 * <p>Hibernate {@link Statistics}의 prepared statement 카운터를 이용해
 * "이 호출은 SQL이 정확히 N번 나가야 한다"를 박제한다. N+1이 발생하면
 * 카운트가 늘어나 테스트가 실패하므로, N+1 회귀를 CI에서 자동으로 잡아낸다.
 *
 * <p>전제: 테스트 프로파일에 {@code hibernate.generate_statistics=true}가 켜져 있어야 한다
 * (운영 프로파일에는 켜지 않는다).
 *
 * <p>측정 대상은 보통 자기 트랜잭션을 새로 여는 서비스 메서드 호출이다.
 * 같은 트랜잭션 안에서 시드한 엔티티가 1차 캐시에 남아 카운트가 왜곡될 수 있는
 * 경우에는 {@link #assertQueryCount(EntityManager, long, Runnable)} 호출 전에
 * {@code em.flush(); em.clear();}로 영속성 컨텍스트를 비운다.
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
