package umc.cockple.demo.domain.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<Long, WebSocketSession> memberSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("웹소켓 연결 성공");

        try {
            Long memberId = extractMemberIdFromURL(session);

            if (memberId != null) {
                session.getAttributes().put("memberId", memberId);
                memberSessions.put(memberId, session);

                log.info("사용자 연결 완료 - memberId: {}, 세션 ID: {}", memberId, session.getId());

                sendConnectionSuccessMessage(session, memberId);
            } else {
                log.warn("memberId를 찾을 수 없습니다. 세션을 종료합니다.");
                session.close();
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생", e);
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    }

    // ========== 내부 메서드들 ==========

    private Long extractMemberIdFromURL(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("memberId=")) {
            try {
                String memberIdStr = query.split("memberId=")[1].split("&")[0]; // Id값 뽑아내기.
                return Long.parseLong(memberIdStr);
            } catch (Exception e) {
                log.error("memberId 추출 실패", e);
            }
        }
        return null;
    }

    private void sendConnectionSuccessMessage(WebSocketSession session, Long memberId) {
        try {
            WebSocketMessageDTO.ConnectionInfo connectionInfo = WebSocketMessageDTO.ConnectionInfo.builder()
                    .type("CONNECTION")
                    .memberId(memberId)
                    .connectedAt(LocalDateTime.now())
                    .message("WebSocket 연결이 성공했습니다.")
                    .build();

            String messageJson = objectMapper.writeValueAsString(connectionInfo);
            session.sendMessage(new TextMessage(messageJson));

            log.info("연결 성공 메시지 전송 완료 - memberId: {}", memberId);
        } catch (Exception e) {
            log.error("연결 성공 메시지 전송 실패", e);
        }
    }

}
