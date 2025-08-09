package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final ChatWebSocketService chatWebSocketService;

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleChatMessageSend(ChatMessageSendEvent event) {
        log.info("메시지 전송 이벤트 처리 - 채팅방: {}, 발신자: {}",
                event.chatRoomId(), event.senderId());

        try {
            chatWebSocketService.sendMessage(event.chatRoomId(), event.content(), event.senderId());
        } catch (Exception e) {
            log.error("메시지 전송 이벤트 처리 중 오류 발생", e);
        }
    }

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) //트랜잭션이 커밋된 후에 실행
    @Async
    public void handlePartyMemberChanged(PartyMemberJoinedEvent event) {
        switch (event.action()) {
            case JOINED -> chatWebSocketService.sendSystemMessage(event.partyId(),
                    event.memberName() + "님이 모임에 참여하셨습니다.");
            case LEFT -> chatWebSocketService.sendSystemMessage(event.partyId(),
                    event.memberName() + "님이 모임을 떠나셨습니다.");
        }
    }
}
