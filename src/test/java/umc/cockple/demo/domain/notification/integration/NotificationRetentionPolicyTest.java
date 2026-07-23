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
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.service.NotificationCommandService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 보관 정책(슬랙 기반) 동작 검증. 실제 삭제 후보 조회 쿼리를 DB에 대해 실행한다.
 *
 * 정책: 보유 알림이 트리거(60)를 넘으면 CAP(50)까지 초과분을 한 번에 정리한다.
 * INVITE는 생성 7일 이내면 삭제 후보에서 제외한다.
 */
@Transactional
@DisplayName("알림 보관 정책")
class NotificationRetentionPolicyTest extends IntegrationTestBase {

    @Autowired NotificationCommandService notificationCommandService;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired NotificationRepository notificationRepository;

    @PersistenceContext EntityManager em;

    @Test
    @DisplayName("트리거를 크게 초과해 쌓여도 한 번의 생성으로 CAP 근처까지 정리된다")
    void overflow_convergesToCapAfterSingleCreate() {
        Member member = seedMember(8201L);
        Party party = seedParty(member, "정리모임");
        for (int i = 0; i < 200; i++) {
            seedNotification(member, party.getId(), NotificationType.SIMPLE);
        }
        em.flush();

        notificationCommandService.createNotification(create(member, party));

        // 200건 → 초과분(200-50=150) 삭제 → 50건 + 신규 1건 = 51건
        assertThat(notificationRepository.countByMember(member)).isEqualTo(51L);
    }

    @Test
    @DisplayName("트리거를 넘어도 최근 INVITE는 삭제 후보에서 제외되어 보존된다")
    void recentInviteSurvivesCleanup() {
        Member member = seedMember(8202L);
        Party party = seedParty(member, "초대보존모임");
        for (int i = 0; i < 55; i++) {
            seedNotification(member, party.getId(), NotificationType.SIMPLE);
        }
        for (int i = 0; i < 10; i++) {
            seedNotification(member, party.getId(), NotificationType.INVITE);   // createdAt = now (최근)
        }
        em.flush();

        notificationCommandService.createNotification(create(member, party));

        // 65건 → 초과분 15건은 전부 SIMPLE에서 삭제, 최근 INVITE 10건은 보존
        assertThat(countByType(member, NotificationType.INVITE)).isEqualTo(10L);
        assertThat(notificationRepository.countByMember(member)).isEqualTo(51L);
    }

    @Test
    @DisplayName("트리거를 넘어도 삭제 후보가 없으면(전부 최근 INVITE) 정리하지 않아 CAP를 넘을 수 있다")
    void noDeletableCandidates_exceedsCap() {
        Member member = seedMember(8203L);
        Party party = seedParty(member, "초대과다모임");
        for (int i = 0; i < 60; i++) {
            seedNotification(member, party.getId(), NotificationType.INVITE);   // 전부 최근 → 보호됨
        }
        em.flush();

        notificationCommandService.createNotification(create(member, party));

        // 삭제 없이 신규만 추가 → 61건. 페이지네이션(이슈 B)이 조회를 방어한다.
        assertThat(notificationRepository.countByMember(member)).isEqualTo(61L);
    }

    @Test
    @DisplayName("생성 7일이 지난 INVITE는 삭제 후보에 포함되어 정리된다")
    void oldInviteIsDeletable() {
        Member member = seedMember(8204L);
        Party party = seedParty(member, "오래된초대모임");
        for (int i = 0; i < 60; i++) {
            seedNotification(member, party.getId(), NotificationType.INVITE);
        }
        em.flush();
        backdateAllNotifications(member, 8);   // 전부 8일 전으로 → 7일 경과, 삭제 후보가 됨
        em.clear();

        notificationCommandService.createNotification(create(member, party));

        // 60건(전부 7일 경과 INVITE) → 초과분 10건 삭제 → INVITE 50건 + 신규 1건 = 51건
        assertThat(notificationRepository.countByMember(member)).isEqualTo(51L);
        assertThat(countByType(member, NotificationType.INVITE)).isEqualTo(50L);
    }

    // === 헬퍼 ===

    private CreateNotificationRequestDTO create(Member member, Party party) {
        return CreateNotificationRequestDTO.builder()
                .member(member)
                .partyId(party.getId())
                .target(NotificationTarget.PARTY_MODIFY)
                .build();
    }

    private Member seedMember(long socialId) {
        return memberRepository.save(
                MemberFixture.createMember("보관정책" + socialId, Gender.MALE, Level.A, socialId));
    }

    private Party seedParty(Member member, String name) {
        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("경기도", name));
        return partyRepository.save(PartyFixture.createParty(name, member.getId(), addr));
    }

    private void seedNotification(Member member, Long partyId, NotificationType type) {
        notificationRepository.save(Notification.builder()
                .member(member)
                .partyId(partyId)
                .title("알림")
                .content("내용")
                .type(type)
                .isRead(false)
                .build());
    }

    private long countByType(Member member, NotificationType type) {
        return em.createQuery(
                        "SELECT count(n) FROM Notification n WHERE n.member = :member AND n.type = :type", Long.class)
                .setParameter("member", member)
                .setParameter("type", type)
                .getSingleResult();
    }

    // @CreatedDate는 persist 시점에 세팅되므로, 오래된 알림 상황은 bulk update로 생성일을 소급한다.
    private void backdateAllNotifications(Member member, int days) {
        em.createQuery("UPDATE Notification n SET n.createdAt = :ts WHERE n.member = :member")
                .setParameter("ts", LocalDateTime.now().minusDays(days))
                .setParameter("member", member)
                .executeUpdate();
    }
}
