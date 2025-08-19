package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.service.websocket.ChatSendService;
import umc.cockple.demo.domain.chat.service.websocket.SubscriptionService;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final ChatSendService chatSendService;
    private final SubscriptionService subscriptionService;

    @EventListener
    @Async
    public void handleChatMessageSend(ChatMessageSendEvent event) {
        log.info("메시지 전송 이벤트 처리 - 채팅방: {}, 발신자: {}",
                event.chatRoomId(), event.senderId());
        try {
            chatSendService
                    .sendMessage(event.chatRoomId(), event.content(), event.files(), event.images(), event.senderId());
        } catch (Exception e) {
            log.error("메시지 전송 이벤트 처리 중 오류 발생", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) //트랜잭션이 커밋된 후에 실행
    @Async
    public void handlePartyMemberChanged(PartyMemberJoinedEvent event) {
        switch (event.action()) {
            case JOINED -> chatSendService.sendSystemMessage(event.partyId(),
                    event.memberName() + "님이 모임에 참여하셨습니다.");
            case LEFT -> chatSendService.sendSystemMessage(event.partyId(),
                    event.memberName() + "님이 모임을 떠나셨습니다.");
        }
    }

    @EventListener
    public void handleChatRoomSubscription(ChatRoomSubscriptionEvent event) {
        log.info("채팅방 구독 이벤트 처리 - 채팅방: {}, 사용자: {}, 액션: {}",
                event.chatRoomId(), event.memberId(), event.action());

        try {
            switch (event.action()) {
                case "SUBSCRIBE" -> {
                    subscriptionService.subscribeToChatRoom(event.chatRoomId(), event.memberId());
                    log.info("사용자 {}가 채팅방 {}를 구독했습니다.", event.memberId(), event.chatRoomId());
                }
                case "UNSUBSCRIBE" -> {
                    subscriptionService.unsubscribeToChatRoom(event.chatRoomId(), event.memberId());
                    log.info("사용자 {}가 채팅방 {}를 구독해제했습니다.", event.memberId(), event.chatRoomId());
                }
                default -> log.warn("알 수 없는 구독 액션: {}", event.action());
            }
        } catch (Exception e) {
            log.error("채팅방 구독 이벤트 처리 중 오류 발생", e);
        }
    }
}
