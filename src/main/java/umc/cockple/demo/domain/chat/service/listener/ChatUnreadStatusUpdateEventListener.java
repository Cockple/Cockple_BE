package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.service.ChatUnreadQueryService;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatUnreadStatusUpdateEventListener {

    private final ChatMessageSender chatMessageSender;
    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatSessionRegistry chatSessionRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleChatUnreadStatusUpdate(ChatUnreadStatusUpdateEvent event) {
        log.info("채팅 안읽음 상태 업데이트 이벤트 처리 시작 - 대상자: {}명", event.targetMemberIds().size());

        List<Long> openMemberIds = chatSessionRegistry.findOpenMemberIds(event.targetMemberIds());
        if (openMemberIds.isEmpty()) {
            log.debug("열린 WebSocket 세션이 있는 안읽음 상태 업데이트 대상 없음");
            return;
        }

        for (Long memberId : openMemberIds) {
            try {
                boolean hasPartyUnread = chatUnreadQueryService.hasPartyUnreadMessages(memberId);
                boolean hasDirectUnread = chatUnreadQueryService.hasDirectUnreadMessages(memberId);

                WebSocketMessageDTO.UnreadStatusUpdateMessage message =
                        WebSocketMessageDTO.UnreadStatusUpdateMessage.builder()
                                .type(WebSocketMessageType.UNREAD_STATUS_UPDATE)
                                .hasUnread(hasPartyUnread || hasDirectUnread)
                                .hasPartyUnread(hasPartyUnread)
                                .hasDirectUnread(hasDirectUnread)
                                .timestamp(LocalDateTime.now())
                                .build();

                chatMessageSender.send(memberId, message);
            } catch (Exception e) {
                log.error("채팅 안읽음 상태 업데이트 처리 실패 - 멤버: {}", memberId, e);
            }
        }
    }
}
