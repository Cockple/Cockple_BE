package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final ChatWebSocketService chatWebSocketService;

    @EventListener
    @Async
    public void handlePartyMemberJoined(PartyMemberJoinedEvent event) {
        String message = switch(event.action()){
            case JOINED -> event.memberName() + "님이 모임에 참여했습니다.";
            case LEFT -> event.memberName() + "님이 모임을 떠났습니다.";
        };

        chatWebSocketService.sendSystemMessage(event.partyId(), message);
    }

}
