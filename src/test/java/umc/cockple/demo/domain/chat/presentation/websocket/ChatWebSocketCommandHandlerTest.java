package umc.cockple.demo.domain.chat.presentation.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.events.ChatListSubscriptionEvent;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.service.ChatValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketCommandHandler")
class ChatWebSocketCommandHandlerTest {

    @Mock private ChatValidator chatValidator;
    @Mock private WebSocketResponseSender webSocketResponseSender;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WebSocketSession session;

    private ChatWebSocketCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new ChatWebSocketCommandHandler(
                chatValidator,
                webSocketResponseSender,
                eventPublisher
        );
    }

    @Test
    @DisplayName("SEND 요청을 검증한 뒤 메시지 전송 이벤트를 발행한다")
    void handleSend_publishesChatMessageSendEvent() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SEND,
                chatRoomId,
                null,
                "hello",
                List.of(),
                null
        );

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(chatValidator).should().validateSendRequest(chatRoomId, "hello", List.of(), memberId);

        ArgumentCaptor<ChatMessageSendEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSendEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatMessageSendEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(event.senderId()).isEqualTo(memberId);
        assertThat(event.content()).isEqualTo("hello");
        assertThat(event.files()).isEmpty();

        then(webSocketResponseSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUBSCRIBE 요청을 검증한 뒤 채팅방 구독 이벤트를 발행하고 ACK를 보낸다")
    void handleSubscribe_publishesChatRoomSubscriptionEventAndSendsAck() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SUBSCRIBE,
                chatRoomId,
                null,
                null,
                null,
                null
        );

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(chatValidator).should().validateSubscriptionRequest(chatRoomId, memberId);

        ArgumentCaptor<ChatRoomSubscriptionEvent> eventCaptor = ArgumentCaptor.forClass(ChatRoomSubscriptionEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatRoomSubscriptionEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.action()).isEqualTo("SUBSCRIBE");

        then(webSocketResponseSender).should().sendSubscriptionMessage(session, chatRoomId, "SUBSCRIBE");
    }

    @Test
    @DisplayName("UNSUBSCRIBE 요청을 검증한 뒤 채팅방 구독 해제 이벤트를 발행하고 ACK를 보낸다")
    void handleUnsubscribe_publishesChatRoomSubscriptionEventAndSendsAck() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.UNSUBSCRIBE,
                chatRoomId,
                null,
                null,
                null,
                null
        );

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(chatValidator).should().validateUnsubscriptionRequest(chatRoomId, memberId);

        ArgumentCaptor<ChatRoomSubscriptionEvent> eventCaptor = ArgumentCaptor.forClass(ChatRoomSubscriptionEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatRoomSubscriptionEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.action()).isEqualTo("UNSUBSCRIBE");

        then(webSocketResponseSender).should().sendSubscriptionMessage(session, chatRoomId, "UNSUBSCRIBE");
    }

    @Test
    @DisplayName("SUBSCRIBE_CHAT_LIST 요청을 검증한 뒤 목록 구독 이벤트를 발행하고 ACK를 보낸다")
    void handleSubscribeChatList_publishesChatListSubscriptionEventAndSendsAck() {
        // given
        Long memberId = 10L;
        List<Long> chatRoomIds = List.of(20L, 30L);
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SUBSCRIBE_CHAT_LIST,
                null,
                chatRoomIds,
                null,
                null,
                null
        );

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(chatValidator).should().validateChatListSubscriptionRequest(memberId, chatRoomIds);

        ArgumentCaptor<ChatListSubscriptionEvent> eventCaptor = ArgumentCaptor.forClass(ChatListSubscriptionEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatListSubscriptionEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.chatRoomIds()).containsExactlyElementsOf(chatRoomIds);
        assertThat(event.action()).isEqualTo("SUBSCRIBE");

        then(webSocketResponseSender).should()
                .sendChatListSubscriptionMessage(session, chatRoomIds, "SUBSCRIBE_CHAT_LIST");
    }

    @Test
    @DisplayName("UNSUBSCRIBE_CHAT_LIST 요청을 검증한 뒤 목록 구독 해제 이벤트를 발행하고 ACK를 보낸다")
    void handleUnsubscribeChatList_publishesChatListSubscriptionEventAndSendsAck() {
        // given
        Long memberId = 10L;
        List<Long> chatRoomIds = List.of(20L, 30L);
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.UNSUBSCRIBE_CHAT_LIST,
                null,
                chatRoomIds,
                null,
                null,
                null
        );

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(chatValidator).should().validateChatListUnsubscriptionRequest(memberId, chatRoomIds);

        ArgumentCaptor<ChatListSubscriptionEvent> eventCaptor = ArgumentCaptor.forClass(ChatListSubscriptionEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatListSubscriptionEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.chatRoomIds()).containsExactlyElementsOf(chatRoomIds);
        assertThat(event.action()).isEqualTo("UNSUBSCRIBE");

        then(webSocketResponseSender).should()
                .sendChatListSubscriptionMessage(session, chatRoomIds, "UNSUBSCRIBE_CHAT_LIST");
    }

    @Test
    @DisplayName("처리 대상이 아닌 타입이면 UNKNOWN_TYPE 오류 응답을 보낸다")
    void handle_sendsUnknownTypeError_whenRequestTypeIsNotCommand() {
        // given
        Long memberId = 10L;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.ERROR,
                null,
                null,
                null,
                null,
                null
        );

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "UNKNOWN_TYPE", "알 수 없는 메시지 타입입니다:" + WebSocketMessageType.ERROR);
        then(chatValidator).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
