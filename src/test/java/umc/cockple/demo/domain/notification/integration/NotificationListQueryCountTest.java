package umc.cockple.demo.domain.notification.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.service.NotificationQueryService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.MemberFixture;

import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

/**
 * 알림 목록(커서 페이지네이션) 조회의 쿼리 수 고정 — N+1 회귀 방지.
 *
 * 커서 조회는 알림 건수와 무관하게 항상 3쿼리여야 한다:
 * 회원 조회 + 커서 조회(findPageByMember) + 전체 개수(countByMember).
 * DTO 매핑은 로딩된 컬럼만 사용하므로 지연 로딩/추가 쿼리가 없다.
 */
@Transactional
@DisplayName("알림 목록 조회 쿼리 카운트")
class NotificationListQueryCountTest extends IntegrationTestBase {

    private static final int LIST_QUERY_COUNT = 3;

    @Autowired NotificationQueryService notificationQueryService;
    @Autowired MemberRepository memberRepository;
    @Autowired NotificationRepository notificationRepository;

    @PersistenceContext EntityManager em;

    @Test
    @DisplayName("알림 개수와 무관하게 고정된 쿼리 수(3)만 실행한다")
    void getAllNotifications_noNPlusOne() {
        Member member = memberRepository.save(
                MemberFixture.createMember("목록측정", Gender.MALE, Level.A, 8401L));
        for (int i = 0; i < 30; i++) {
            seedNotification(member, i);
        }
        em.flush();

        assertQueryCount(em, LIST_QUERY_COUNT, () ->
                notificationQueryService.getAllNotifications(member.getId(), null, 20));
    }

    private void seedNotification(Member member, int idx) {
        notificationRepository.save(Notification.builder()
                .member(member)
                .partyId(100L)
                .title("알림" + idx)
                .content("내용" + idx)
                .type(NotificationType.SIMPLE)
                .isRead(false)
                .build());
    }
}
