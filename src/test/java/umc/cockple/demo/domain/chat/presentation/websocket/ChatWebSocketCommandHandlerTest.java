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
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.events.ChatListSubscriptionEvent;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.service.ChatValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

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
    @DisplayName("SEND 검증 실패 시 오류 응답을 보내고 이벤트를 발행하지 않는다")
    void handleSend_sendsErrorResponse_whenValidationFails() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        ChatErrorCode errorCode = ChatErrorCode.CHAT_ROOM_ACCESS_DENIED;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SEND,
                chatRoomId,
                null,
                "hello",
                List.of(),
                null
        );
        willThrow(new ChatException(errorCode))
                .given(chatValidator)
                .validateSendRequest(chatRoomId, "hello", List.of(), memberId);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        thenChatErrorResponseSent(errorCode);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUBSCRIBE 검증 실패 시 오류 응답을 보내고 이벤트를 발행하지 않는다")
    void handleSubscribe_sendsErrorResponse_whenValidationFails() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        ChatErrorCode errorCode = ChatErrorCode.CHAT_ROOM_ACCESS_DENIED;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SUBSCRIBE,
                chatRoomId,
                null,
                null,
                null,
                null
        );
        willThrow(new ChatException(errorCode))
                .given(chatValidator)
                .validateSubscriptionRequest(chatRoomId, memberId);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        thenChatErrorResponseSent(errorCode);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("UNSUBSCRIBE 검증 실패 시 오류 응답을 보내고 이벤트를 발행하지 않는다")
    void handleUnsubscribe_sendsErrorResponse_whenValidationFails() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        ChatErrorCode errorCode = ChatErrorCode.CHAT_ROOM_ACCESS_DENIED;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.UNSUBSCRIBE,
                chatRoomId,
                null,
                null,
                null,
                null
        );
        willThrow(new ChatException(errorCode))
                .given(chatValidator)
                .validateUnsubscriptionRequest(chatRoomId, memberId);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        thenChatErrorResponseSent(errorCode);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUBSCRIBE_CHAT_LIST 검증 실패 시 오류 응답을 보내고 이벤트를 발행하지 않는다")
    void handleSubscribeChatList_sendsErrorResponse_whenValidationFails() {
        // given
        Long memberId = 10L;
        List<Long> chatRoomIds = List.of(20L, 30L);
        ChatErrorCode errorCode = ChatErrorCode.CHAT_ROOM_ACCESS_DENIED;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.SUBSCRIBE_CHAT_LIST,
                null,
                chatRoomIds,
                null,
                null,
                null
        );
        willThrow(new ChatException(errorCode))
                .given(chatValidator)
                .validateChatListSubscriptionRequest(memberId, chatRoomIds);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        thenChatErrorResponseSent(errorCode);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("UNSUBSCRIBE_CHAT_LIST 검증 실패 시 오류 응답을 보내고 이벤트를 발행하지 않는다")
    void handleUnsubscribeChatList_sendsErrorResponse_whenValidationFails() {
        // given
        Long memberId = 10L;
        List<Long> chatRoomIds = List.of(20L, 30L);
        ChatErrorCode errorCode = ChatErrorCode.CHAT_ROOM_ACCESS_DENIED;
        WebSocketMessageDTO.Request request = new WebSocketMessageDTO.Request(
                WebSocketMessageType.UNSUBSCRIBE_CHAT_LIST,
                null,
                chatRoomIds,
                null,
                null,
                null
        );
        willThrow(new ChatException(errorCode))
                .given(chatValidator)
                .validateChatListUnsubscriptionRequest(memberId, chatRoomIds);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        thenChatErrorResponseSent(errorCode);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SEND 처리 중 예상치 못한 예외 발생 시 SEND_MESSAGE_ERROR 응답을 보낸다")
    void handleSend_sendsFallbackError_whenUnexpectedExceptionOccurs() {
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
        willThrow(new IllegalStateException("boom"))
                .given(chatValidator)
                .validateSendRequest(chatRoomId, "hello", List.of(), memberId);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "SEND_MESSAGE_ERROR", "메시지 전송 처리 중 오류가 발생했습니다.");
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUBSCRIBE 처리 중 예상치 못한 예외 발생 시 SUBSCRIPTION_ERROR 응답을 보낸다")
    void handleSubscribe_sendsFallbackError_whenUnexpectedExceptionOccurs() {
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
        willThrow(new IllegalStateException("boom"))
                .given(chatValidator)
                .validateSubscriptionRequest(chatRoomId, memberId);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다.");
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("UNSUBSCRIBE 처리 중 예상치 못한 예외 발생 시 UNSUBSCRIPTION_ERROR 응답을 보낸다")
    void handleUnsubscribe_sendsFallbackError_whenUnexpectedExceptionOccurs() {
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
        willThrow(new IllegalStateException("boom"))
                .given(chatValidator)
                .validateUnsubscriptionRequest(chatRoomId, memberId);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "UNSUBSCRIPTION_ERROR", "구독 해제 처리 중 오류가 발생했습니다.");
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUBSCRIBE_CHAT_LIST 처리 중 예상치 못한 예외 발생 시 SUBSCRIPTION_ERROR 응답을 보낸다")
    void handleSubscribeChatList_sendsFallbackError_whenUnexpectedExceptionOccurs() {
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
        willThrow(new IllegalStateException("boom"))
                .given(chatValidator)
                .validateChatListSubscriptionRequest(memberId, chatRoomIds);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "SUBSCRIPTION_ERROR", "채팅방 목록 구독 처리 중 오류가 발생했습니다.");
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("UNSUBSCRIBE_CHAT_LIST 처리 중 예상치 못한 예외 발생 시 UNSUBSCRIPTION_ERROR 응답을 보낸다")
    void handleUnsubscribeChatList_sendsFallbackError_whenUnexpectedExceptionOccurs() {
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
        willThrow(new IllegalStateException("boom"))
                .given(chatValidator)
                .validateChatListUnsubscriptionRequest(memberId, chatRoomIds);

        // when
        commandHandler.handle(session, request, memberId);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "UNSUBSCRIPTION_ERROR", "채팅방 목록 구독 해제 처리 중 오류가 발생했습니다.");
        then(eventPublisher).shouldHaveNoInteractions();
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

    private void thenChatErrorResponseSent(ChatErrorCode errorCode) {
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, errorCode.getReason().getCode(), errorCode.getReason().getMessage());
    }
}
