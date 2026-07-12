package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.service.websocket.send.ChatSendService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageSendEventListener {

    private final ChatSendService chatSendService;

    @EventListener
    @Async("chatExecutor")
    public void handleChatMessageSend(ChatMessageSendEvent event) {
        log.info("메시지 전송 이벤트 처리 - 채팅방: {}, 발신자: {}",
                event.chatRoomId(), event.senderId());
        try {
            chatSendService.sendMessage(event.chatRoomId(), event.content(), event.files(), event.senderId());
        } catch (Exception e) {
            log.error("메시지 전송 이벤트 처리 중 오류 발생", e);
        }
    }
}
