package umc.cockple.demo.global.realtime.logging;

import org.slf4j.MDC;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

import java.util.Map;

public final class WebSocketMdcSupport {

    public static final String WS_SESSION_ID = "wsSessionId";

    private WebSocketMdcSupport() {
    }

    public static MdcScope open(WebSocketSession session) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        putSessionMdc(session);
        return new MdcScope(previousContext);
    }

    public static MdcScope open(Long memberId) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        putMemberId(memberId);
        return new MdcScope(previousContext);
    }

    private static void putSessionMdc(WebSocketSession session) {
        if (session == null) {
            MDC.remove(WS_SESSION_ID);
            MDC.remove(WebSocketSessionAttributes.MEMBER_ID);
            return;
        }

        String sessionId = session.getId();
        if (sessionId == null || sessionId.isBlank()) {
            MDC.remove(WS_SESSION_ID);
        } else {
            MDC.put(WS_SESSION_ID, sessionId);
        }

        Map<String, Object> attributes = session.getAttributes();
        Object memberId = attributes == null ? null : attributes.get(WebSocketSessionAttributes.MEMBER_ID);
        if (memberId == null) {
            MDC.remove(WebSocketSessionAttributes.MEMBER_ID);
        } else {
            MDC.put(WebSocketSessionAttributes.MEMBER_ID, String.valueOf(memberId));
        }
    }

    private static void putMemberId(Long memberId) {
        if (memberId == null) {
            MDC.remove(WebSocketSessionAttributes.MEMBER_ID);
        } else {
            MDC.put(WebSocketSessionAttributes.MEMBER_ID, String.valueOf(memberId));
        }
        MDC.remove(WS_SESSION_ID);
    }

    public static final class MdcScope implements AutoCloseable {
        private final Map<String, String> previousContext;

        private MdcScope(Map<String, String> previousContext) {
            this.previousContext = previousContext;
        }

        @Override
        public void close() {
            if (previousContext == null || previousContext.isEmpty()) {
                MDC.clear();
                return;
            }
            MDC.setContextMap(previousContext);
        }
    }
}
