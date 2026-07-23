package umc.cockple.demo.domain.notification.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
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

import java.util.ArrayList;
import java.util.List;

import static umc.cockple.demo.support.QueryCountAssert.assertQueryCount;

/**
 * 모임 전체 알림(createRoleNotification) 경로의 쿼리 비용 기준선.
 *
 * createRoleNotification은 한 트랜잭션 안에서 회원마다 createNotification을 호출한다.
 * 이 테스트는 그 루프를 동일하게 재현해, 회원 수(N)에 따라 쿼리가 어떻게 늘어나는지 박제한다.
 */
@Transactional
@DisplayName("모임 전체 알림 생성 경로 쿼리 기준선")
class NotificationBulkCreateQueryCountTest extends IntegrationTestBase {

    private static final int MEMBER_COUNT = 5;

    /**
     * N명에게 알림을 생성할 때의 총 쿼리 수.
     * party 조회 1회(1차 캐시) + 회원마다 (countByMember 1 + insert 1) = 1 + 2N.
     */
    private static final int BULK_CREATE_QUERY_COUNT = 1 + 2 * MEMBER_COUNT;

    @Autowired NotificationCommandService notificationCommandService;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;

    @PersistenceContext EntityManager em;

    @Test
    @DisplayName("N명에게 알림을 생성하면 party는 1회만 조회하고 회원당 count+insert(=2N)만 추가된다")
    void bulkCreate_partyLoadedOnce_perMemberCountAndInsert() {
        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("경기도", "벌크측정동"));
        Party party = partyRepository.save(PartyFixture.createParty("벌크측정모임", 1L, addr));

        List<Member> members = new ArrayList<>();
        for (int i = 0; i < MEMBER_COUNT; i++) {
            members.add(memberRepository.save(
                    MemberFixture.createMember("벌크측정" + i, Gender.MALE, Level.A, 8300L + i)));
        }
        em.flush();

        assertQueryCount(em, BULK_CREATE_QUERY_COUNT, () -> {
            for (Member member : members) {
                notificationCommandService.createNotification(CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(party.getId())
                        .target(NotificationTarget.PARTY_MODIFY)
                        .build());
            }
        });
    }
}
