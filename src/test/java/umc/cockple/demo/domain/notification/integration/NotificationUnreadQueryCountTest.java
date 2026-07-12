package umc.cockple.demo.domain.notification.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.service.NotificationQueryService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

// 안 읽은 알림 존재여부 조회 최적화(알림 전량 로딩 → EXISTS 1회) 회귀 방지 테스트
@DisplayName("안 읽은 알림 존재여부 조회 쿼리 카운트 테스트")
class NotificationUnreadQueryCountTest extends IntegrationTestBase {

    /**
     * checkUnreadNotification의 기대 쿼리 수
     * 회원/알림 컬렉션 로딩 없이 EXISTS 한 번만 실행하므로, 알림 개수와 무관하게 1이어야 한다.
     */
    private static final int UNREAD_CHECK_QUERY_COUNT = 1;

    @Autowired NotificationQueryService notificationQueryService;
    @Autowired MemberRepository memberRepository;
    @Autowired NotificationRepository notificationRepository;

    @PersistenceContext EntityManager em;

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("알림 개수와 무관하게 항상 1개의 쿼리만 실행한다")
    void checkUnreadNotification_runsSingleQuery() {
        Member member = createMember(9001L);
        // 읽은 알림 5, 안 읽은 알림 3
        for (int i = 0; i < 5; i++) seedNotification(member, true, i);
        for (int i = 0; i < 3; i++) seedNotification(member, false, i);

        assertQueryCount(em, UNREAD_CHECK_QUERY_COUNT, () ->
                notificationQueryService.checkUnreadNotification(member.getId()));
    }

    @Test
    @DisplayName("안 읽은 알림이 있으면 true, 모두 읽었으면 false를 반환한다")
    void checkUnreadNotification_returnsCorrectResult() {
        Member hasUnread = createMember(9002L);
        seedNotification(hasUnread, true, 0);
        seedNotification(hasUnread, false, 1);

        Member allRead = createMember(9003L);
        seedNotification(allRead, true, 0);
        seedNotification(allRead, true, 1);

        Member noNotifications = createMember(9004L);

        assertThat(check(hasUnread)).isTrue();
        assertThat(check(allRead)).isFalse();
        assertThat(check(noNotifications)).isFalse();
    }

    // === 헬퍼 ===

    private boolean check(Member member) {
        ExistNewNotificationResponseDTO response =
                notificationQueryService.checkUnreadNotification(member.getId());
        return response.existNewNotification();
    }

    private Member createMember(long socialId) {
        return memberRepository.save(
                MemberFixture.createMember("회원" + socialId, Gender.MALE, Level.A, socialId));
    }

    private void seedNotification(Member member, boolean isRead, int idx) {
        notificationRepository.save(Notification.builder()
                .member(member)
                .partyId(100L)
                .title("알림" + idx)
                .content("내용" + idx)
                .type(NotificationType.SIMPLE)
                .isRead(isRead)
                .build());
    }
}
