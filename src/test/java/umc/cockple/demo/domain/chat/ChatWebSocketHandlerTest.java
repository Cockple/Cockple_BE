package umc.cockple.demo.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.handler.ChatWebSocketHandler;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatWebSocketHandlerTest {

    @InjectMocks
    private ChatWebSocketHandler handler;

    @Mock
    private ChatWebSocketService chatWebSocketService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        sessionAttributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(session.getId()).thenReturn("test-session-id");
    }

    @Test
    @Order(1)
    @DisplayName("WebSocket 연결 성공 - 정상적인 memberId")
    void afterConnectionEstablished_Success() throws Exception {
        // Given
        URI uri = URI.create("ws://localhost:8080/ws/chats?memberId=123");
        when(session.getUri()).thenReturn(uri);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        handler.afterConnectionEstablished(session);

        // Then
        assertThat(sessionAttributes.get("memberId")).isEqualTo(123L); // 세션에 memberId 저장됨
        verify(session).sendMessage(any(TextMessage.class)); // 연결 성공 메시지 전송됨
    }
}
