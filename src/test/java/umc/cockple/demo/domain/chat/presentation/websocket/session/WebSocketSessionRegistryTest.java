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
    @DisplayName("등록된 세션과 같은 세션을 제거하면 조회되지 않는다")
    void remove_deletesRegisteredSessionWhenSameSession() {
        // given
        Long memberId = 10L;
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        sessionRegistry.register(memberId, session);

        // when
        sessionRegistry.remove(memberId, session);

        // then
        assertThat(sessionRegistry.findOpenSession(memberId)).isEmpty();
    }

    @Test
    @DisplayName("오래된 세션 제거 요청은 최신 세션을 제거하지 않는다")
    void remove_doesNotDeleteNewSessionWhenOldSessionCloses() {
        // given
        Long memberId = 10L;
        WebSocketSession oldSession = mock(WebSocketSession.class);
        WebSocketSession newSession = mock(WebSocketSession.class);
        given(newSession.isOpen()).willReturn(true);

        sessionRegistry.register(memberId, oldSession);
        sessionRegistry.register(memberId, newSession);

        // when
        sessionRegistry.remove(memberId, oldSession);

        // then
        assertThat(sessionRegistry.findOpenSession(memberId)).contains(newSession);
    }

    @Test
    @DisplayName("닫힌 세션 조회 시 같은 세션일 때만 저장소에서 정리한다")
    void findOpenSession_removesClosedSessionOnlyWhenStillRegistered() {
        // given
        Long memberId = 10L;
        WebSocketSession closedSession = mock(WebSocketSession.class);
        given(closedSession.isOpen()).willReturn(false);
        sessionRegistry.register(memberId, closedSession);

        // when
        assertThat(sessionRegistry.findOpenSession(memberId)).isEmpty();

        // then
        assertThat(sessionRegistry.findOpenSession(memberId)).isEmpty();
    }
}
