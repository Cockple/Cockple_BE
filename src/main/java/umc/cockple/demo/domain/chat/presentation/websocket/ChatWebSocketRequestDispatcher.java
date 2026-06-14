package umc.cockple.demo.domain.chat.presentation.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketRequestDispatcher {

    private final ObjectMapper objectMapper;
    private final WebSocketResponseSender webSocketResponseSender;
    private final ChatWebSocketCommandHandler commandHandler;

    public void dispatch(WebSocketSession session, String payload) {
        log.info("메시지 수신 - 세션 ID: {}, payloadSize: {}", session.getId(), payload == null ? 0 : payload.length());

        try {
            WebSocketMessageDTO.Request request = objectMapper.readValue(
                    payload, WebSocketMessageDTO.Request.class
            );

            Long memberId = (Long) session.getAttributes().get("memberId");
            if (memberId == null) {
                webSocketResponseSender.sendErrorMessage(session, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
                return;
            }

            log.info("메시지 타입: {}, 채팅방 ID: {}, 사용자 ID: {}", memberId, session.getId(), memberId);
            commandHandler.handle(session, request, memberId);

        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생", e);
            webSocketResponseSender.sendErrorMessage(session, "PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다.");
        }
    }
}
