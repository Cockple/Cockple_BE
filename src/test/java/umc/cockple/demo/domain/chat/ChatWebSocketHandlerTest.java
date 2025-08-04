package umc.cockple.demo.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.handler.ChatWebSocketHandler;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;

import java.util.HashMap;
import java.util.Map;

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
        when(session.isOpen()).thenReturn(true);
    }
}
