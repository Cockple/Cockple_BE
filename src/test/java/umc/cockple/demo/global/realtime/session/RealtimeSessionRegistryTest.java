package umc.cockple.demo.global.realtime.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("RealtimeSessionRegistry")
class RealtimeSessionRegistryTest {

    private static final RealtimeEndpoint LEGACY_CHAT = RealtimeEndpoint.of("legacy-chat");
    private static final RealtimeEndpoint REALTIME = RealtimeEndpoint.of("realtime");

    private final RealtimeSessionRegistry registry = new RealtimeSessionRegistry();

    @Test
    @DisplayName("같은 멤버의 서로 다른 endpoint 세션을 독립적으로 보관한다")
    void registerKeepsSessionsForDifferentEndpoints() {
        WebSocketSession chatSession = openSession("chat-session");
        WebSocketSession realtimeSession = openSession("realtime-session");

        registry.register(10L, LEGACY_CHAT, chatSession);
        registry.register(10L, REALTIME, realtimeSession);

        assertThat(registry.findLatestOpenSession(10L, LEGACY_CHAT)).contains(chatSession);
        assertThat(registry.findLatestOpenSession(10L, REALTIME)).contains(realtimeSession);
    }

    @Test
    @DisplayName("같은 endpoint의 열린 세션들을 최신 등록 순서로 조회한다")
    void findOpenSessionsReturnsAllSessionsNewestFirst() {
        WebSocketSession first = openSession("session-first");
        WebSocketSession second = openSession("session-second");

        registry.register(10L, LEGACY_CHAT, first);
        registry.register(10L, LEGACY_CHAT, second);

        assertThat(registry.findOpenSessions(10L, LEGACY_CHAT))
                .extracting(RealtimeSession::webSocketSession)
                .containsExactly(second, first);
    }

    @Test
    @DisplayName("한 endpoint 세션 제거가 같은 멤버의 다른 endpoint 세션에 영향을 주지 않는다")
    void removeDoesNotAffectOtherEndpointSession() {
        WebSocketSession chatSession = openSession("chat-session");
        WebSocketSession realtimeSession = openSession("realtime-session");
        registry.register(10L, LEGACY_CHAT, chatSession);
        registry.register(10L, REALTIME, realtimeSession);

        registry.remove(10L, chatSession);

        assertThat(registry.findLatestOpenSession(10L, LEGACY_CHAT)).isEmpty();
        assertThat(registry.findLatestOpenSession(10L, REALTIME)).contains(realtimeSession);
    }

    @Test
    @DisplayName("같은 session ID가 재등록되면 오래된 세션 제거 요청이 새 세션을 지우지 않는다")
    void removeOldSessionIdentityDoesNotDeleteReplacementSession() {
        WebSocketSession oldSession = openSession("same-session-id");
        WebSocketSession replacementSession = openSession("same-session-id");
        registry.register(10L, LEGACY_CHAT, oldSession);
        registry.register(10L, LEGACY_CHAT, replacementSession);

        registry.remove(10L, oldSession);

        assertThat(registry.findLatestOpenSession(10L, LEGACY_CHAT)).contains(replacementSession);
    }

    @Test
    @DisplayName("요청한 endpoint에 열린 세션이 있는 멤버만 필터링한다")
    void findOpenMemberIdsFiltersByEndpoint() {
        registry.register(10L, LEGACY_CHAT, openSession("chat-session"));
        registry.register(20L, REALTIME, openSession("realtime-session"));
        registry.register(30L, LEGACY_CHAT, closedSession("closed-chat-session"));

        List<Long> openChatMemberIds =
                registry.findOpenMemberIds(List.of(10L, 20L, 30L, 40L), LEGACY_CHAT);

        assertThat(openChatMemberIds).containsExactly(10L);
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
