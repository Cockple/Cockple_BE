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
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.chat.service.WebSocketBroadcastService;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatWebSocketService chatWebSocketService;
    private final WebSocketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("웹소켓 연결 성공");

        try {
            Long memberId = extractMemberIdFromURL(session);

            if (memberId != null) {
                session.getAttributes().put("memberId", memberId);
                broadcastService.addSession(memberId, session);
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
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("메시지 수신");
        log.info("메시지: {}", message.getPayload());

        try {
            WebSocketMessageDTO.Request request = objectMapper.readValue(
                    message.getPayload(), WebSocketMessageDTO.Request.class
            );

            Long memberId = (Long) session.getAttributes().get("memberId");
            if (memberId == null) {
                sendErrorMessage(session, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
                return;
            }

            log.info("메시지 타입: {}, 채팅방 ID: {}, 사용자 ID: {}", memberId, session.getId(), memberId);

            switch (request.type()) {
                case SEND:
                    handleSendMessage(session, request, memberId);
                    break;
                default:
                    sendErrorMessage(session, "UNKNOWN_TYPE", "알 수 없는 메시지 타입입니다:" + request.type());
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생", e);
            sendErrorMessage(session, "PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다:" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");

        log.info("웹소켓 연결 종료");
        log.info("세션 ID: {}, 사용자 ID: {}, 종료 상태: {}", session.getId(), memberId, status);

        if (memberId != null) {
            broadcastService.removeSession(memberId);
            log.info("사용자 세션 정리 완료 - memberId: {}", memberId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");
        log.error("WebSocket 전송 오류 발생 - 세션 ID: {}, 사용자 ID: {}", session.getId(), memberId, exception);
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
                    .type(WebSocketMessageType.CONNECT)
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

    private void sendErrorMessage(WebSocketSession session, String errorCode, String message) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 에러 메시지를 전송할 수 없습니다.");
            return;
        }

        try {
            WebSocketMessageDTO.ErrorResponse errorResponse = WebSocketMessageDTO.ErrorResponse.builder()
                    .type(WebSocketMessageType.ERROR)
                    .errorCode(errorCode)
                    .message(message)
                    .build();

            String errorJson = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(errorJson));

            log.info("에러 메시지 전송 완료 - 코드: {}, 메시지: {}", errorCode, message);
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패", e);
        }
    }

    // ========== 메시지 처리 메서드들 ==========

    private void handleSendMessage(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        log.info("메시지 전송 처리 - 채팅방 ID: {}, 사용자 ID: {}, 내용: {}",
                request.chatRoomId(), memberId, request.content());

        try {
            chatWebSocketService.sendMessage(request.chatRoomId(), request.content(), memberId);
        } catch (ChatException e) {
            log.error("메시지 전송 중 오류 발생", e);
            sendErrorMessage(session, e.getCode().toString(), e.getMessage());
        }
    }
}
