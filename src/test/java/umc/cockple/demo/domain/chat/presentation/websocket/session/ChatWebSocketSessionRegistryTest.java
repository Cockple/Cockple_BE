package umc.cockple.demo.domain.chat.presentation.websocket.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketEndpoint;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("ChatWebSocketSessionRegistry")
class ChatWebSocketSessionRegistryTest {

    private final RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
    private final ChatWebSocketSessionRegistry sessionRegistry =
            new ChatWebSocketSessionRegistry(realtimeSessionRegistry);

    @Test
    @DisplayName("멤버의 가장 최근 legacy 채팅 세션을 조회한다")
    void findOpenSessionReturnsLatestRegisteredChatSession() {
        Long memberId = 10L;
        WebSocketSession oldSession = openSession("session-old");
        WebSocketSession newSession = openSession("session-new");

        sessionRegistry.register(memberId, oldSession);
        sessionRegistry.register(memberId, newSession);

        assertThat(sessionRegistry.findOpenSession(memberId)).contains(newSession);
    }

    @Test
    @DisplayName("열린 legacy 채팅 세션이 있는 멤버만 필터링한다")
    void findOpenMemberIdsFiltersMembersWithOpenChatSessions() {
        sessionRegistry.register(10L, openSession("session-open"));
        realtimeSessionRegistry.register(
                20L,
                RealtimeWebSocketEndpoint.SESSION_ENDPOINT,
                openSession("realtime-session-open")
        );
        sessionRegistry.register(30L, closedSession("session-closed"));

        List<Long> openMemberIds = sessionRegistry.findOpenMemberIds(List.of(10L, 20L, 30L, 40L));

        assertThat(openMemberIds).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("등록된 세션과 같은 세션을 제거하면 이전 열린 세션으로 복귀한다")
    void removeLatestSessionFallsBackToPreviousOpenSession() {
        Long memberId = 10L;
        WebSocketSession oldSession = openSession("session-old");
        WebSocketSession newSession = openSession("session-new");
        sessionRegistry.register(memberId, oldSession);
        sessionRegistry.register(memberId, newSession);

        sessionRegistry.remove(memberId, newSession);

        assertThat(sessionRegistry.findOpenSession(memberId)).contains(oldSession);
    }

    @Test
    @DisplayName("오래된 세션 종료는 최신 세션을 제거하지 않는다")
    void removeOldSessionDoesNotDeleteLatestSession() {
        Long memberId = 10L;
        WebSocketSession oldSession = openSession("session-old");
        WebSocketSession newSession = openSession("session-new");
        sessionRegistry.register(memberId, oldSession);
        sessionRegistry.register(memberId, newSession);

        sessionRegistry.remove(memberId, oldSession);

        assertThat(sessionRegistry.findOpenSession(memberId)).contains(newSession);
    }

    @Test
    @DisplayName("닫힌 세션은 조회 대상에서 제외하고 저장소에서 정리한다")
    void findOpenSessionRemovesClosedSession() {
        Long memberId = 10L;
        sessionRegistry.register(memberId, closedSession("session-closed"));

        assertThat(sessionRegistry.findOpenSession(memberId)).isEmpty();
        assertThat(sessionRegistry.findOpenSession(memberId)).isEmpty();
    }

    private WebSocketSession openSession(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.isOpen()).willReturn(true);
        return session;
    }

    private WebSocketSession closedSession(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.isOpen()).willReturn(false);
        return session;
    }
}
