package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.presentation.websocket.session.WebSocketSessionMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketResponseSender {

    private final ChatMessageEncoder messageEncoder;
    private final WebSocketSessionMessageSender sessionMessageSender;

    public void sendConnectionSuccessMessage(WebSocketSession session, MemberConnectionInfo memberInfo) {
        WebSocketMessageDTO.ConnectionInfo connectionInfo = WebSocketMessageDTO.ConnectionInfo.builder()
                .type(WebSocketMessageType.CONNECT)
                .memberId(memberInfo.memberId())
                .memberName(memberInfo.memberName())
                .timestamp(LocalDateTime.now())
                .message("WebSocket 연결이 성공했습니다.")
                .build();

        sendMessage(session, connectionInfo);
    }

    public void sendErrorMessage(WebSocketSession session, String errorCode, String message) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 에러 메시지를 전송할 수 없습니다.");
            return;
        }

        WebSocketMessageDTO.ErrorResponse errorResponse = WebSocketMessageDTO.ErrorResponse.builder()
                .type(WebSocketMessageType.ERROR)
                .errorCode(errorCode)
                .message(message)
                .build();

        sendMessage(session, errorResponse);
    }

    public void sendSubscriptionMessage(WebSocketSession session, Long chatRoomId, String action) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 구독 응답 메시지를 전송할 수 없습니다.");
            return;
        }

        WebSocketMessageDTO.SubscriptionResponse subscriptionResponse = WebSocketMessageDTO.SubscriptionResponse.builder()
                .type(WebSocketMessageType.valueOf(action))
                .chatRoomId(chatRoomId)
                .message(action.equals("SUBSCRIBE") ? "채팅방 구독이 완료되었습니다." : "채팅방 구독이 해제되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(session, subscriptionResponse);
    }

    public void sendChatListSubscriptionMessage(WebSocketSession session, List<Long> chatRoomIds, String action) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 채팅방 목록 구독 응답 메시지를 전송할 수 없습니다.");
            return;
        }

        WebSocketMessageType messageType;
        String message;

        switch (action) {
            case "SUBSCRIBE_CHAT_LIST":
                messageType = WebSocketMessageType.SUBSCRIBE_CHAT_LIST;
                message = String.format("채팅방 목록 구독이 완료되었습니다. (총 %d개)", chatRoomIds.size());
                break;
            case "UNSUBSCRIBE_CHAT_LIST":
                messageType = WebSocketMessageType.UNSUBSCRIBE_CHAT_LIST;
                message = "채팅방 목록 구독이 해제되었습니다.";
                break;
            default:
                log.error("알 수 없는 채팅방 목록 구독 액션: {}", action);
                return;
        }

        WebSocketMessageDTO.ChatListSubscriptionResponse response = WebSocketMessageDTO.ChatListSubscriptionResponse.builder()
                .type(messageType)
                .chatRoomIds(chatRoomIds)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(session, response);
    }

    private void sendMessage(WebSocketSession session, Object message) {
        EncodedChatMessage encodedMessage = messageEncoder.encode(message).orElse(null);
        if (encodedMessage == null) {
            log.error("WebSocket 응답 메시지 인코딩 실패");
            return;
        }

        sessionMessageSender.send(session, encodedMessage);
    }
}
