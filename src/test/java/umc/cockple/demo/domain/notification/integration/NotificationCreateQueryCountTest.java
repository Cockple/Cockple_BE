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

import static umc.cockple.demo.support.QueryCountAssert.assertEntityLoadCount;
import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

@Transactional
@DisplayName("알림 생성 경로 쿼리/엔티티 로드 기준선")
class NotificationCreateQueryCountTest extends IntegrationTestBase {

    private static final int SEEDED_NOTIFICATIONS = 50;

    /** createNotification 1회가 실행하는 SQL 수 */
    // countByMember + findFirst(삭제 대상) + delete + party findById + insert = 5
    private static final int CREATE_QUERY_COUNT = 5;

    // 알림 전량 로딩이 사라지고, 삭제 대상 1건 + party 1건만 로딩
    private static final int CREATE_ENTITY_LOAD_COUNT = 2;

    @Autowired NotificationCommandService notificationCommandService;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired NotificationRepository notificationRepository;

    @PersistenceContext EntityManager em;

    @Test
    @DisplayName("알림 50건 보유 회원에게 알림을 생성해도 보관 개수 판정에 알림을 전량 로딩하지 않는다")
    void createNotification_doesNotLoadAllNotificationsForCapCheck() {
        Member member = memberRepository.save(
                MemberFixture.createMember("알림측정", Gender.MALE, Level.A, 8101L));
        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("경기도", "알림측정동"));
        Party party = partyRepository.save(PartyFixture.createParty("알림측정모임", member.getId(), addr));

        for (int i = 0; i < SEEDED_NOTIFICATIONS; i++) {
            seedNotification(member, party.getId(), i);
        }
        em.flush();

        CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                .member(member)
                .partyId(party.getId())
                .target(NotificationTarget.PARTY_MODIFY)
                .build();

        // 캡 로직이 "1건 삭제 + 1건 삽입"이라 두 번 실행해도 보유 건수는 50으로 유지
        assertEntityLoadCount(em, CREATE_ENTITY_LOAD_COUNT, () ->
                notificationCommandService.createNotification(dto));
        assertQueryCount(em, CREATE_QUERY_COUNT, () ->
                notificationCommandService.createNotification(dto));
    }

    private void seedNotification(Member member, Long partyId, int idx) {
        notificationRepository.save(Notification.builder()
                .member(member)
                .partyId(partyId)
                .title("알림" + idx)
                .content("내용" + idx)
                .type(NotificationType.SIMPLE)   // 비-INVITE라야 캡 초과 시 삭제 후보
                .isRead(false)
                .build());
    }
}
