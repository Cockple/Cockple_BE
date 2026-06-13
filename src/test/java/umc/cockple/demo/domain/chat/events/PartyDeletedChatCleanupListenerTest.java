package umc.cockple.demo.domain.chat.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.service.ChatRoomService;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyDeletedChatCleanupListener")
class PartyDeletedChatCleanupListenerTest {

    @InjectMocks
    private PartyDeletedChatCleanupListener listener;

    @Mock
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("모임 삭제 이벤트를 받으면 채팅방 삭제 서비스에 위임한다")
    void handlePartyDeleted_delegatesToChatRoomService() {
        Long partyId = 1L;
        Long deletedByMemberId = 10L;
        PartyDeletedEvent event = PartyDeletedEvent.deleted(partyId, deletedByMemberId);

        listener.handlePartyDeleted(event);

        verify(chatRoomService).deletePartyChatRoom(partyId);
    }
}
