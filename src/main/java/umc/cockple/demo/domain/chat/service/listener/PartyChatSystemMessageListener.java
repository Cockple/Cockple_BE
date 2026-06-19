package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.service.websocket.send.ChatSendService;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

@Component
@RequiredArgsConstructor
public class PartyChatSystemMessageListener {

    private final ChatSendService chatSendService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePartyMemberChanged(PartyMemberJoinedEvent event) {
        switch (event.action()) {
            case JOINED -> chatSendService.sendSystemMessage(event.partyId(),
                    event.memberName() + "님이 모임에 참여하셨습니다.");
            case LEFT -> chatSendService.sendSystemMessage(event.partyId(),
                    event.memberName() + "님이 모임을 떠나셨습니다.");
        }
    }
}
