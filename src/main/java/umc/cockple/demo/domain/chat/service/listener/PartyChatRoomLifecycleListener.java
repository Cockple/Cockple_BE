package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.service.command.PartyChatRoomLifecycleService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.events.PartyCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartyChatRoomLifecycleListener {

    private final PartyChatRoomLifecycleService partyChatRoomLifecycleService;
    private final PartyLookupService partyLookupService;
    private final MemberLookupService memberLookupService;

    @EventListener
    public void handlePartyCreated(PartyCreatedEvent event) {
        log.info("모임 생성 이벤트 처리 - partyId: {}, ownerId: {}", event.partyId(), event.ownerId());
        Party party = partyLookupService.findByIdOrThrow(event.partyId());
        Member owner = memberLookupService.findByIdOrThrow(event.ownerId());
        partyChatRoomLifecycleService.createPartyChatRoom(party, owner);
    }

    @EventListener
    public void handlePartyMemberChanged(PartyMemberJoinedEvent event) {
        log.info("모임 멤버 변경 이벤트 처리 - partyId: {}, memberId: {}, action: {}",
                event.partyId(), event.memberId(), event.action());

        switch (event.action()) {
            case JOINED -> {
                Member member = memberLookupService.findByIdOrThrow(event.memberId());
                partyChatRoomLifecycleService.joinPartyChatRoom(event.partyId(), member);
            }
            case LEFT -> partyChatRoomLifecycleService.leavePartyChatRoom(event.partyId(), event.memberId());
        }
    }

    @EventListener
    public void handlePartyDeleted(PartyDeletedEvent event) {
        log.info("모임 삭제 이벤트 처리 - partyId: {}, deletedBy: {}", event.partyId(), event.deletedByMemberId());
        partyChatRoomLifecycleService.deletePartyChatRoom(event.partyId());
    }
}
