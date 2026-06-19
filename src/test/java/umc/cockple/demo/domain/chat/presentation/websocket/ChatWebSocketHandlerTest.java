package umc.cockple.demo.domain.chat.presentation.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.presentation.websocket.session.WebSocketSessionRegistry;
import umc.cockple.demo.domain.member.service.MemberQueryService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketHandler")
class ChatWebSocketHandlerTest {

    @Mock private MemberQueryService memberQueryService;
    @Mock private WebSocketResponseSender webSocketResponseSender;
    @Mock private ChatWebSocketRequestDispatcher requestDispatcher;
    @Mock private WebSocketSessionRegistry sessionRegistry;
    @Mock private WebSocketSession session;

    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(
                memberQueryService,
                webSocketResponseSender,
                requestDispatcher,
                sessionRegistry
        );
    }

    @Test
    @DisplayName("연결 종료 시 닫힌 세션과 일치할 때만 세션 저장소에서 제거한다")
    void afterConnectionClosed_removesClosedSessionByIdentity() {
        // given
        Long memberId = 10L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", memberId);
        given(session.getAttributes()).willReturn(attributes);
        given(session.getId()).willReturn("session-1");

        // when
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // then
        then(sessionRegistry).should().remove(memberId, session);
    }
}
