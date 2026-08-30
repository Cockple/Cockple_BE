package umc.cockple.demo.domain.chat.service.websocket.command;

import java.util.List;

public interface ChatCommandResponder {

    void sendError(String errorCode, String message);

    void acknowledgeRoomSubscription(Long chatRoomId, String action);

    void acknowledgeChatListSubscription(List<Long> chatRoomIds, String action);
}
