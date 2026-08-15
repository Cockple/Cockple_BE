package umc.cockple.demo.domain.chat.service.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.service.command.PartyChatRoomLifecycleService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyDeletedChatCleanupListener")
class PartyDeletedChatCleanupListenerTest {

    @InjectMocks
    private PartyDeletedChatCleanupListener listener;

    @Mock
    private PartyChatRoomLifecycleService partyChatRoomLifecycleService;

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
