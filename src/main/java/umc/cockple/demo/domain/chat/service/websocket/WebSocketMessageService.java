package umc.cockple.demo.domain.chat.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketMessageService {

    private final ObjectMapper objectMapper;

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

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
        }
    }
}
