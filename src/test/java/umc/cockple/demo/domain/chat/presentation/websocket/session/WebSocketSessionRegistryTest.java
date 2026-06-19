package umc.cockple.demo.domain.chat.presentation.websocket.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketSessionRegistry")
class WebSocketSessionRegistryTest {

    private final WebSocketSessionRegistry sessionRegistry = new WebSocketSessionRegistry();

    @Test
    @DisplayName("멤버별 열린 세션을 저장하고 조회한다")
    void findOpenSession_returnsRegisteredOpenSession() {
        // given
        Long memberId = 10L;
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);

        // when
        sessionRegistry.register(memberId, session);

        // then
        assertThat(sessionRegistry.findOpenSession(memberId)).contains(session);
    }

    @Test
    @DisplayName("열린 세션이 있는 멤버만 필터링한다")
    void findOpenMemberIds_filtersMembersWithOpenSessions() {
        // given
        WebSocketSession openSession = mock(WebSocketSession.class);
        WebSocketSession closedSession = mock(WebSocketSession.class);
        given(openSession.isOpen()).willReturn(true);
        given(closedSession.isOpen()).willReturn(false);

        sessionRegistry.register(10L, openSession);
        sessionRegistry.register(20L, closedSession);

        // when
        List<Long> openMemberIds = sessionRegistry.findOpenMemberIds(List.of(10L, 20L, 30L));

        // then
        assertThat(openMemberIds).containsExactly(10L);
    }

    @Test
    @DisplayName("세션을 제거하면 조회되지 않는다")
    void remove_deletesRegisteredSession() {
        // given
        Long memberId = 10L;
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        sessionRegistry.register(memberId, session);

        // when
        sessionRegistry.remove(memberId);

        // then
        assertThat(sessionRegistry.findOpenSession(memberId)).isEmpty();
    }
}
