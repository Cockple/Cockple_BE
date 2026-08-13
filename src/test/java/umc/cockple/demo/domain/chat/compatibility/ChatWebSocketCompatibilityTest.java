package umc.cockple.demo.domain.chat.compatibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.presentation.realtime.ChatRealtimeDomainHandler;
import umc.cockple.demo.domain.chat.presentation.websocket.ChatWebSocketCommandHandler;
import umc.cockple.demo.domain.chat.presentation.websocket.ChatWebSocketRequestDispatcher;
import umc.cockple.demo.domain.chat.presentation.websocket.WebSocketResponseSender;
import umc.cockple.demo.domain.chat.presentation.websocket.session.ChatWebSocketSessionRegistry;
import umc.cockple.demo.domain.chat.presentation.websocket.session.WebSocketMessageSender;
import umc.cockple.demo.domain.chat.service.ChatValidator;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageFanout;
import umc.cockple.demo.global.realtime.config.RealtimeWebSocketProperties;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.message.JacksonRealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.publish.WebSocketRealtimeMessagePublisher;
import umc.cockple.demo.global.realtime.routing.RealtimeMessageRouter;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketEndpoint;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketRequestDispatcher;
import umc.cockple.demo.global.realtime.transport.WebSocketRealtimeResponderFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("기존·공용 채팅 WebSocket 호환성")
class ChatWebSocketCompatibilityTest {

    private ObjectMapper objectMapper;
    private ChatValidator chatValidator;
    private ApplicationEventPublisher eventPublisher;
    private ChatWebSocketCommandHandler commandHandler;
    private RealtimeMessageEncoder messageEncoder;
    private WebSocketSessionMessageSender sessionMessageSender;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        chatValidator = mock(ChatValidator.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        commandHandler = new ChatWebSocketCommandHandler(chatValidator, eventPublisher);
        messageEncoder = new JacksonRealtimeMessageEncoder(objectMapper);
        sessionMessageSender = new WebSocketSessionMessageSender();
    }

    @Test
    @DisplayName("공용 SUBSCRIBE 요청은 requestId를 유지한 표준 envelope ACK를 반환한다")
    void realtimeSubscribeReturnsCorrelatedEnvelope() throws Exception {
        WebSocketSession session = authenticatedOpenSession("realtime-session", 10L);
        RealtimeWebSocketRequestDispatcher dispatcher = realtimeDispatcher();
        String payload = """
                {
                  "version": 1,
                  "domain": "CHAT",
                  "action": "SUBSCRIBE",
                  "requestId": "request-1",
                  "payload": {"chatRoomId": 20}
                }
                """;

        dispatcher.dispatch(session, payload);

        ArgumentCaptor<ChatRoomSubscriptionEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatRoomSubscriptionEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().chatRoomId()).isEqualTo(20L);
        assertThat(eventCaptor.getValue().memberId()).isEqualTo(10L);

        JsonNode response = capturedJson(session);
        assertThat(response.get("version").asInt()).isEqualTo(1);
        assertThat(response.get("domain").asText()).isEqualTo("CHAT");
        assertThat(response.get("type").asText()).isEqualTo("SUBSCRIBE");
        assertThat(response.get("requestId").asText()).isEqualTo("request-1");
        assertThat(response.get("data").get("chatRoomId").asLong()).isEqualTo(20L);
        assertThat(response.has("error")).isFalse();
    }

    @Test
    @DisplayName("공용 SEND 요청은 기존 채팅 메시지 전송 이벤트를 그대로 발행한다")
    void realtimeSendPublishesExistingChatCommandEvent() throws Exception {
        WebSocketSession session = authenticatedOpenSession("realtime-session", 10L);
        RealtimeWebSocketRequestDispatcher dispatcher = realtimeDispatcher();
        String payload = """
                {
                  "version": 1,
                  "domain": "CHAT",
                  "action": "SEND",
                  "requestId": "request-2",
                  "payload": {
                    "chatRoomId": 20,
                    "content": "hello",
                    "images": []
                  }
                }
                """;

        dispatcher.dispatch(session, payload);

        ArgumentCaptor<ChatMessageSendEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatMessageSendEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatMessageSendEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(20L);
        assertThat(event.senderId()).isEqualTo(10L);
        assertThat(event.content()).isEqualTo("hello");
        then(session).should(never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("기존 SUBSCRIBE 요청과 ACK는 version·domain·requestId 없는 legacy JSON을 유지한다")
    void legacySubscribePreservesLegacyContract() throws Exception {
        WebSocketSession session = authenticatedOpenSession("legacy-session", 10L);
        WebSocketResponseSender responseSender = new WebSocketResponseSender(
                messageEncoder,
                sessionMessageSender
        );
        ChatWebSocketRequestDispatcher dispatcher = new ChatWebSocketRequestDispatcher(
                objectMapper,
                responseSender,
                commandHandler
        );

        dispatcher.dispatch(session, "{\"type\":\"SUBSCRIBE\",\"chatRoomId\":20}");

        JsonNode response = capturedJson(session);
        assertThat(response.get("type").asText()).isEqualTo("SUBSCRIBE");
        assertThat(response.get("chatRoomId").asLong()).isEqualTo(20L);
        assertThat(response.get("message").asText()).isEqualTo("채팅방 구독이 완료되었습니다.");
        assertThat(response.has("version")).isFalse();
        assertThat(response.has("domain")).isFalse();
        assertThat(response.has("requestId")).isFalse();
    }

    @Test
    @DisplayName("서버 push는 legacy 세션에는 기존 JSON, realtime 세션에는 표준 envelope로 전달한다")
    void serverPushUsesEndpointSpecificContracts() throws Exception {
        Long memberId = 10L;
        WebSocketSession legacySession = authenticatedOpenSession("legacy-session", memberId);
        WebSocketSession realtimeSession = authenticatedOpenSession("realtime-session", memberId);
        RealtimeSessionRegistry sessionRegistry = new RealtimeSessionRegistry();
        ChatWebSocketSessionRegistry chatSessionRegistry =
                new ChatWebSocketSessionRegistry(sessionRegistry);
        chatSessionRegistry.register(memberId, legacySession);
        sessionRegistry.register(
                memberId,
                RealtimeWebSocketEndpoint.SESSION_ENDPOINT,
                realtimeSession
        );
        WebSocketMessageSender legacySender = new WebSocketMessageSender(
                chatSessionRegistry,
                sessionMessageSender
        );
        WebSocketRealtimeMessagePublisher realtimePublisher =
                new WebSocketRealtimeMessagePublisher(
                        sessionRegistry,
                        messageEncoder,
                        sessionMessageSender
                );
        ChatMessageFanout fanout = new ChatMessageFanout(legacySender, realtimePublisher);
        WebSocketMessageDTO.MessageResponse message = WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(20L)
                .messageId(100L)
                .content("hello")
                .messageType(MessageType.TEXT)
                .images(List.of())
                .senderId(30L)
                .senderName("sender")
                .timestamp(LocalDateTime.of(2026, 8, 10, 12, 30))
                .unreadCount(1)
                .build();
        EncodedRealtimeMessage legacyMessage = messageEncoder.encode(message).orElseThrow();

        boolean delivered = fanout.send(memberId, legacyMessage, message.type(), message);

        assertThat(delivered).isTrue();
        JsonNode legacyJson = capturedJson(legacySession);
        assertThat(legacyJson.get("type").asText()).isEqualTo("SEND");
        assertThat(legacyJson.get("chatRoomId").asLong()).isEqualTo(20L);
        assertThat(legacyJson.has("version")).isFalse();
        assertThat(legacyJson.has("domain")).isFalse();

        JsonNode realtimeJson = capturedJson(realtimeSession);
        assertThat(realtimeJson.get("version").asInt()).isEqualTo(1);
        assertThat(realtimeJson.get("domain").asText()).isEqualTo("CHAT");
        assertThat(realtimeJson.get("type").asText()).isEqualTo("SEND");
        assertThat(realtimeJson.has("requestId")).isFalse();
        assertThat(realtimeJson.get("data").get("chatRoomId").asLong()).isEqualTo(20L);
        assertThat(realtimeJson.get("data").get("messageId").asLong()).isEqualTo(100L);
    }

    private RealtimeWebSocketRequestDispatcher realtimeDispatcher() {
        ChatRealtimeDomainHandler chatHandler = new ChatRealtimeDomainHandler(
                objectMapper,
                commandHandler
        );
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(chatHandler));
        WebSocketRealtimeResponderFactory responderFactory =
                new WebSocketRealtimeResponderFactory(messageEncoder, sessionMessageSender);
        return new RealtimeWebSocketRequestDispatcher(
                objectMapper,
                router,
                responderFactory,
                new RealtimeWebSocketProperties()
        );
    }

    private WebSocketSession authenticatedOpenSession(String sessionId, Long memberId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketSessionAttributes.MEMBER_ID, memberId);
        attributes.put(WebSocketSessionAttributes.AUTHENTICATED, true);
        given(session.getId()).willReturn(sessionId);
        given(session.getAttributes()).willReturn(attributes);
        given(session.isOpen()).willReturn(true);
        return session;
    }

    private JsonNode capturedJson(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        then(session).should().sendMessage(messageCaptor.capture());
        return objectMapper.readTree(messageCaptor.getValue().getPayload());
    }
}
