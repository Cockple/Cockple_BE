package umc.cockple.demo.global.realtime.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import umc.cockple.demo.global.realtime.logging.WebSocketMdcSupport;
import umc.cockple.demo.global.realtime.protocol.RealtimeConnectionInfo;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

// 인증 통과 후 생명주기 관리
@Component
@Slf4j
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeSessionRegistry sessionRegistry;
    private final RealtimeWebSocketRequestDispatcher requestDispatcher;
    private final WebSocketRealtimeResponderFactory responderFactory;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(session)) {
            Long memberId = getAuthenticatedMemberId(session);
            if (memberId == null) {
                log.warn("인증 정보가 없는 실시간 WebSocket 세션을 종료합니다. - sessionId: {}", session.getId());
                session.close();
                return;
            }

            try {
                sessionRegistry.register(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT, session); // 세션등록
                RealtimeResponder responder = responderFactory.createInfrastructureResponder(session);
                responder.send("CONNECTED", new RealtimeConnectionInfo(memberId, session.getId())); // 연결 후 서버->클라 CONNECTED 전송
                log.info("실시간 WebSocket 연결 완료 - memberId: {}, sessionId: {}", memberId, session.getId());
            } catch (Exception e) {
                sessionRegistry.remove(memberId, session);
                log.error("실시간 WebSocket 연결 처리 실패 - memberId: {}, sessionId: {}", memberId, session.getId(), e);
                session.close();
            }
        }
    }

    // 메세지 수신
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        requestDispatcher.dispatch(session, message.getPayload());
    }

    // 연결 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(session)) {
            Long memberId = getMemberId(session);
            sessionRegistry.remove(memberId, session);
            log.info("실시간 WebSocket 연결 종료 - memberId: {}, sessionId: {}, status: {}", memberId, session.getId(), status);
        }
    }

    // 전송 오류
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(session)) {
            log.error("실시간 WebSocket 전송 오류 - sessionId: {}", session.getId(), exception);
        }
    }

    private Long getAuthenticatedMemberId(WebSocketSession session) {
        Boolean authenticated = (Boolean) session.getAttributes().get(WebSocketSessionAttributes.AUTHENTICATED);
        return Boolean.TRUE.equals(authenticated) ? getMemberId(session) : null;
    }

    private Long getMemberId(WebSocketSession session) {
        Object memberId = session.getAttributes().get(WebSocketSessionAttributes.MEMBER_ID);
        return memberId instanceof Long value ? value : null;
    }
}
