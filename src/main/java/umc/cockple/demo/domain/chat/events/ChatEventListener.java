package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.service.ChatRoomService;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final ChatRoomService chatRoomService;

    @EventListener
    @Async
    public void handlePartyMemberChanged(PartyMemberJoinedEvent event) {
        log.info("모임 멤버 변경 처리 - partyId: {}, memberId: {}, actions: {}",
                event.partyId(), event.memberId(), event.action());

        switch (event.action()) {
            case JOINED -> {
                chatRoomService.joinPartyChatRoom(event.partyId(), event.memberId());
                chatRoomService.sendSystemMessage(event.partyId(),
                        event.memberName() + "님이 모임에 참여하셨습니다.");
            }
            case LEFT -> {
                chatRoomService.leavePartyChatRoom(event.partyId(), event.memberId());
                chatRoomService.sendSystemMessage(event.partyId(),
                        event.memberName() + "님이 모임을 떠나셨습니다.");
            }
        }
    }

}
