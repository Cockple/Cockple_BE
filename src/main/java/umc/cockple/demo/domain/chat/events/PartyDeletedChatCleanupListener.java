package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.service.ChatRoomService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartyDeletedChatCleanupListener {

    private final ChatRoomService chatRoomService;

    @EventListener
    public void handlePartyDeleted(PartyDeletedEvent event) {
        log.info("모임 삭제 이벤트 처리 - partyId: {}, deletedBy: {}", event.partyId(), event.deletedByMemberId());
        chatRoomService.deletePartyChatRoom(event.partyId());
    }
}
