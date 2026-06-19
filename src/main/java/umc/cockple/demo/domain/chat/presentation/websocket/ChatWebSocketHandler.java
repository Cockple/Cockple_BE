package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.presentation.websocket.session.WebSocketSessionRegistry;
import umc.cockple.demo.domain.member.service.MemberQueryService;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MemberQueryService memberQueryService;
    private final WebSocketResponseSender webSocketResponseSender;
    private final ChatWebSocketRequestDispatcher requestDispatcher;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("웹소켓 연결 성공");

        try {
            Long memberId = (Long) session.getAttributes().get("memberId");
            Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");

            if (memberId != null && Boolean.TRUE.equals(authenticated)) {
                MemberConnectionInfo memberInfo = memberQueryService.getMemberConnectionInfo(memberId);
                session.getAttributes().put("memberName", memberInfo.memberName());

                sessionRegistry.register(memberId, session);
                log.info("사용자 연결 완료 - memberId: {}, 세션 ID: {}", memberId, session.getId());

                webSocketResponseSender.sendConnectionSuccessMessage(session, memberInfo);
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
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        requestDispatcher.dispatch(session, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        Long memberId = (Long) session.getAttributes().get("memberId");

        log.info("웹소켓 연결 종료");
        log.info("세션 ID: {}, 사용자 ID: {}, 종료 상태: {}", session.getId(), memberId, status);

        if (memberId != null) {
            sessionRegistry.remove(memberId, session);
            log.info("사용자 세션 정리 완료 - memberId: {}", memberId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");
        if (isShutdownRelatedError(exception)) {
            log.debug("서버 종료 관련 WebSocket 전송 오류 (정상) - 세션: {}, 사용자: {}", session.getId(), memberId);
        } else {
            log.error("WebSocket 전송 오류 발생 - 세션 ID: {}, 사용자 ID: {}", session.getId(), memberId, exception);
        }
    }

    private boolean isShutdownRelatedError(Throwable exception) {
        if (exception == null) return false;

        String message = exception.getMessage();
        return message != null && (
                message.contains("ClosedChannelException") ||
                message.contains("WebSocket session has been closed")
        );
    }
}
