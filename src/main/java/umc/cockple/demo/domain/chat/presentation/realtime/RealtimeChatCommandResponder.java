package umc.cockple.demo.domain.chat.presentation.realtime;

import lombok.RequiredArgsConstructor;
import umc.cockple.demo.domain.chat.service.websocket.command.ChatCommandResponder;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;

import java.util.List;

@RequiredArgsConstructor
final class RealtimeChatCommandResponder implements ChatCommandResponder {

    private final RealtimeResponder responder;

    @Override
    public void sendError(String errorCode, String message) {
        responder.sendError(errorCode, message);
    }

    @Override
    public void acknowledgeRoomSubscription(Long chatRoomId, String action) {
        responder.send(action, new RoomSubscriptionData(chatRoomId));
    }

    @Override
    public void acknowledgeChatListSubscription(List<Long> chatRoomIds, String action) {
        responder.send(action, new ChatListSubscriptionData(chatRoomIds));
    }

    record RoomSubscriptionData(Long chatRoomId) {
    }

    record ChatListSubscriptionData(List<Long> chatRoomIds) {
    }
}
