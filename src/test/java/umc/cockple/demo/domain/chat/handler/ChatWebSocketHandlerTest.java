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
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(chatWebSocketService, objectMapper);
        sessionAttributes = new HashMap<>();
    }

    @AfterEach
    void tearDown() {
        handler.clearAllSessionsForTest();
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
    }

    @Test
    @Order(4)
    @DisplayName("브로드캐스트 - 채팅방에 참여자들이 있는 경우")
    void broadcastToChatRoom_WithParticipants() throws Exception {
        // Given
        Long chatRoomId = 1L;

        // 세션들 Mock 설정
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session3.isOpen()).thenReturn(false); // 닫힌 세션
        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        when(session3.getId()).thenReturn("session3");

        // 채팅방에 세션들 추가
        handler.addChatRoomSessionForTest(chatRoomId, 100L, session1);
        handler.addChatRoomSessionForTest(chatRoomId, 200L, session2);
        handler.addChatRoomSessionForTest(chatRoomId, 300L, session3);

        WebSocketMessageDTO.Response response = WebSocketMessageDTO.Response.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .content("테스트 메시지")
                .build();

        when(objectMapper.writeValueAsString(response)).thenReturn("{}");

        // When
        handler.broadcastToChatRoomForTest(chatRoomId, response);

        // Then
        verify(session1).sendMessage(any(TextMessage.class)); // 열린 세션에 전송
        verify(session2).sendMessage(any(TextMessage.class)); // 열린 세션에 전송
        verify(session3, never()).sendMessage(any(TextMessage.class)); // 닫힌 세션에는 전송 안함

        // 실패한 세션이 정리되었는지 확인
        assertThat(handler.isMemberInChatRoomForTest(chatRoomId, 300L)).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("완전한 메시지 전송 플로우 - 브로드캐스트까지")
    void completeMessageSendFlow() throws Exception {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 123L;
        sessionAttributes.put("memberId", senderId);

        // 채팅방에 여러 참여자 추가
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");

        handler.addChatRoomSessionForTest(chatRoomId, 100L, session1);
        handler.addChatRoomSessionForTest(chatRoomId, 200L, session2);

        String messagePayload = """
                {
                    "type": "SEND",
                    "chatRoomId": 1,
                    "content": "안녕하세요 모두!"
                }
                """;
        TextMessage textMessage = new TextMessage(messagePayload);

        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SEND, chatRoomId, "안녕하세요 모두!"
        );

        WebSocketMessageDTO.Response response = WebSocketMessageDTO.Response.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .senderName("발신자")
                .senderProfileImageUrl("테스트 이미지 url")
                .content("안녕하세요 모두!")
                .createdAt(LocalDateTime.now())
                .build();

        // Mock 설정
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(objectMapper.readValue(messagePayload, WebSocketMessageDTO.Request.class))
                .thenReturn(request);
        when(chatWebSocketService.sendMessage(chatRoomId, "안녕하세요 모두!", senderId))
                .thenReturn(response);
        when(objectMapper.writeValueAsString(response)).thenReturn("{\"message\":\"broadcast\"}");

        // When
        handler.handleTextMessage(session, textMessage);

        // Then
        // 1. 서비스 호출 확인
        verify(chatWebSocketService).sendMessage(chatRoomId, "안녕하세요 모두!", senderId);

        // 2. 병렬 처리를 위해 timeout 후 로직 수행
        verify(session1, timeout(2000)).sendMessage(any(TextMessage.class));
        verify(session2, timeout(2000)).sendMessage(any(TextMessage.class));
    }

    @Test
    @Order(6)
    @DisplayName("채팅방 구독 성공 - 새로운 사용자")
    void handleSubscribe_NewUser_Success() throws Exception {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 123L;
        sessionAttributes.put("memberId", memberId);

        String subscribePayload = """
            {
                "type": "SUBSCRIBE",
                "chatRoomId": 1,
                "content": null
            }
            """;

        TextMessage textMessage = new TextMessage(subscribePayload);

        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SUBSCRIBE, chatRoomId, null
        );

        // Mock 설정
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(session.getId()).thenReturn("test-session-id");
        when(objectMapper.readValue(subscribePayload, WebSocketMessageDTO.Request.class))
                .thenReturn(request);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        handler.handleTextMessage(session, textMessage);

        // Then
        verify(chatWebSocketService).validateSubscribe(chatRoomId, memberId); // 구독 검증 로직 작동 확인
        assertThat(handler.isMemberInChatRoomForTest(chatRoomId, memberId)).isTrue(); // 멤버가 채팅방에 들어갔는지 확인
        verify(session).sendMessage(any(TextMessage.class)); // 메시지가 정상적으로 전송됐는지 확인
    }

}
