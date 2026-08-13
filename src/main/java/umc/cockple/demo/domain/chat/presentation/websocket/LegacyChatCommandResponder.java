package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.command.ChatCommandResponder;

import java.util.List;

@RequiredArgsConstructor
final class LegacyChatCommandResponder implements ChatCommandResponder {

    private final WebSocketSession session;
    private final WebSocketResponseSender responseSender;

    @Override
    public void sendError(String errorCode, String message) {
        responseSender.sendErrorMessage(session, errorCode, message);
    }

    @Override
    public void acknowledgeRoomSubscription(Long chatRoomId, String action) {
        responseSender.sendSubscriptionMessage(session, chatRoomId, action);
    }

    @Override
    public void acknowledgeChatListSubscription(List<Long> chatRoomIds, String action) {
        responseSender.sendChatListSubscriptionMessage(session, chatRoomIds, action);
    }
}
