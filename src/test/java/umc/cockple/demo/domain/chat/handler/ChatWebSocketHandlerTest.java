package umc.cockple.demo.domain.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.chat.service.WebSocketBroadcastService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatWebSocketHandlerTest {

    private ChatWebSocketHandler handler;

    @Mock
    private ChatWebSocketService chatWebSocketService;

    @Mock
    private WebSocketBroadcastService broadcastService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(chatWebSocketService, broadcastService, objectMapper);
        sessionAttributes = new HashMap<>();
    }

    @Test
    @Order(1)
    @DisplayName("WebSocket 연결 성공 - 정상적인 memberId")
    void afterConnectionEstablished_Success() throws Exception {
        // Given
        URI uri = URI.create("ws://localhost:8080/ws/chats?memberId=123");
        when(session.getUri()).thenReturn(uri);
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(session.getId()).thenReturn("test-session-id");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        handler.afterConnectionEstablished(session);

        // Then
        assertThat(sessionAttributes.get("memberId")).isEqualTo(123L); // 세션에 memberId 저장됨
        verify(broadcastService).addSession(123L, session); // 세션 추가 로직 확인
        verify(session).sendMessage(any(TextMessage.class)); // 연결 성공 메시지 전송됨
    }

    @Test
    @Order(2)
    @DisplayName("WebSocket 연결 실패 - memberId 없음")
    void afterConnectionEstablished_NoMemberId() throws Exception {
        // Given
        URI uri = URI.create("ws://localhost:8080/ws/chats");
        when(session.getUri()).thenReturn(uri);

        // When
        handler.afterConnectionEstablished(session);

        // Then
        verify(session).close(); // 연결 실패 시 세션 닫는 로직
    }

    @Test
    @Order(3)
    @DisplayName("메시지 전송 성공")
    void handleTextMessage_SendSuccess() throws Exception {
        // Given
        sessionAttributes.put("memberId", 123L);

        String messagePayload = """
                {
                    "type": "SEND",
                    "chatRoomId": 1,
                    "content": "안녕하세요!"
                }
                """;
        TextMessage textMessage = new TextMessage(messagePayload);

        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SEND, 1L, "안녕하세요!"
        );

        WebSocketMessageDTO.Response response = WebSocketMessageDTO.Response.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(1L)
                .senderId(123L)
                .senderName("테스트유저")
                .senderProfileImageUrl("테스트 이미지 url")
                .content("안녕하세요!")
                .createdAt(LocalDateTime.now())
                .build();

        when(objectMapper.readValue(messagePayload, WebSocketMessageDTO.Request.class))
                .thenReturn(request);
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(session.getId()).thenReturn("test-session-id");
        when(chatWebSocketService.sendMessage(1L, "안녕하세요!", 123L))
                .thenReturn(response);

        // When
        handler.handleTextMessage(session, textMessage);

        // Then
        verify(chatWebSocketService).sendMessage(1L, "안녕하세요!", 123L);
        verify(broadcastService).broadcastToChatRoom(1L, response);
    }

    @Test
    @Order(4)
    @DisplayName("연결 종료 시 세션 정리")
    void afterConnectionClosed_Success() throws Exception {
        // Given
        sessionAttributes.put("memberId", 123L);
        when(session.getAttributes()).thenReturn(sessionAttributes);

        // When
        handler.afterConnectionClosed(session, null);

        // Then
        verify(broadcastService).removeSession(123L); // 세션 제거 확인
    }
}
