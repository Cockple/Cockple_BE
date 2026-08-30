package umc.cockple.demo.domain.chat.service.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.service.command.PartyChatRoomLifecycleService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.events.PartyCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyChatRoomLifecycleListener")
class PartyChatRoomLifecycleListenerTest {

    @InjectMocks
    private PartyChatRoomLifecycleListener listener;

    @Mock
    private PartyChatRoomLifecycleService partyChatRoomLifecycleService;
    @Mock
    private PartyLookupService partyLookupService;
    @Mock
    private MemberLookupService memberLookupService;

    @Test
    @DisplayName("모임 생성 이벤트를 받으면 모임과 모임장을 조회해 채팅방을 생성한다")
    void handlePartyCreated_createsPartyChatRoom() {
        Long partyId = 1L;
        Long ownerId = 10L;
        Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        Party party = PartyFixture.createParty("모임", ownerId, PartyFixture.createPartyAddr("서울", "강남"));
        PartyCreatedEvent event = PartyCreatedEvent.created(partyId, ownerId);
        given(partyLookupService.findByIdOrThrow(partyId)).willReturn(party);
        given(memberLookupService.findByIdOrThrow(ownerId)).willReturn(owner);

        listener.handlePartyCreated(event);

        verify(partyChatRoomLifecycleService).createPartyChatRoom(party, owner);
    }

    @Test
    @DisplayName("모임 가입 이벤트를 받으면 멤버를 조회해 채팅방에 참여시킨다")
    void handlePartyMemberChanged_joinsPartyChatRoom() {
        Long partyId = 1L;
        Long memberId = 20L;
        Member member = MemberFixture.createMember("신규멤버", Gender.FEMALE, Level.B, 1002L);
        ReflectionTestUtils.setField(member, "id", memberId);
        PartyMemberJoinedEvent event = PartyMemberJoinedEvent.joined(partyId, member);
        given(memberLookupService.findByIdOrThrow(memberId)).willReturn(member);

        listener.handlePartyMemberChanged(event);

        verify(partyChatRoomLifecycleService).joinPartyChatRoom(partyId, member);
    }

    @Test
    @DisplayName("모임 탈퇴 이벤트를 받으면 채팅방에서 해당 멤버를 퇴장시킨다")
    void handlePartyMemberChanged_leavesPartyChatRoom() {
        Long partyId = 1L;
        Long memberId = 20L;
        Member member = MemberFixture.createMember("탈퇴멤버", Gender.MALE, Level.C, 1003L);
        ReflectionTestUtils.setField(member, "id", memberId);
        PartyMemberJoinedEvent event = PartyMemberJoinedEvent.left(partyId, member);

        listener.handlePartyMemberChanged(event);

        verify(partyChatRoomLifecycleService).leavePartyChatRoom(partyId, memberId);
    }

    @Test
    @DisplayName("모임 삭제 이벤트를 받으면 채팅방 삭제 서비스에 위임한다")
    void handlePartyDeleted_delegatesToPartyChatRoomLifecycleService() {
        Long partyId = 1L;
        Long deletedByMemberId = 10L;
        PartyDeletedEvent event = PartyDeletedEvent.deleted(partyId, deletedByMemberId);

        listener.handlePartyDeleted(event);

        verify(partyChatRoomLifecycleService).deletePartyChatRoom(partyId);
    }
}
