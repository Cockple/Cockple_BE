package umc.cockple.demo.domain.chat.presentation.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.presentation.websocket.session.WebSocketSessionMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketResponseSender")
class WebSocketResponseSenderTest {

    @Mock private ChatMessageEncoder messageEncoder;
    @Mock private WebSocketSessionMessageSender sessionMessageSender;
    @Mock private WebSocketSession session;

    private WebSocketResponseSender responseSender;

    @BeforeEach
    void setUp() {
        responseSender = new WebSocketResponseSender(messageEncoder, sessionMessageSender);
    }

    @Test
    @DisplayName("연결 성공 응답을 조립해 인코딩 후 현재 세션에 전송한다")
    void sendConnectionSuccessMessage_encodesAndSendsConnectionInfo() {
        // given
        MemberConnectionInfo memberInfo = new MemberConnectionInfo(10L, "홍길동");
        EncodedChatMessage encodedMessage = new EncodedChatMessage("connection-json");
        given(messageEncoder.encode(any(WebSocketMessageDTO.ConnectionInfo.class)))
                .willReturn(Optional.of(encodedMessage));

        // when
        responseSender.sendConnectionSuccessMessage(session, memberInfo);

        // then
        ArgumentCaptor<WebSocketMessageDTO.ConnectionInfo> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.ConnectionInfo.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        WebSocketMessageDTO.ConnectionInfo message = messageCaptor.getValue();
        assertThat(message.type()).isEqualTo(WebSocketMessageType.CONNECT);
        assertThat(message.memberId()).isEqualTo(memberInfo.memberId());
        assertThat(message.memberName()).isEqualTo(memberInfo.memberName());

        then(sessionMessageSender).should().send(session, encodedMessage);
    }

    @Test
    @DisplayName("열린 세션에 기존 ERROR 응답을 인코딩해 전송한다")
    void sendErrorMessage_encodesAndSendsLegacyErrorResponse() {
        EncodedChatMessage encodedMessage = new EncodedChatMessage("error-json");
        given(session.isOpen()).willReturn(true);
        given(messageEncoder.encode(any(WebSocketMessageDTO.ErrorResponse.class)))
                .willReturn(Optional.of(encodedMessage));

        responseSender.sendErrorMessage(session, "CHAT4001", "채팅방에 접근할 수 없습니다.");

        ArgumentCaptor<WebSocketMessageDTO.ErrorResponse> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.ErrorResponse.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        WebSocketMessageDTO.ErrorResponse message = messageCaptor.getValue();
        assertThat(message.type()).isEqualTo(WebSocketMessageType.ERROR);
        assertThat(message.errorCode()).isEqualTo("CHAT4001");
        assertThat(message.message()).isEqualTo("채팅방에 접근할 수 없습니다.");
        then(sessionMessageSender).should().send(session, encodedMessage);
    }

    @Test
    @DisplayName("기존 채팅방 구독 ACK의 type과 안내 문구를 유지한다")
    void sendSubscriptionMessage_preservesLegacySubscribeAcknowledgement() {
        EncodedChatMessage encodedMessage = new EncodedChatMessage("subscribe-json");
        given(session.isOpen()).willReturn(true);
        given(messageEncoder.encode(any(WebSocketMessageDTO.SubscriptionResponse.class)))
                .willReturn(Optional.of(encodedMessage));

        responseSender.sendSubscriptionMessage(session, 20L, "SUBSCRIBE");

        ArgumentCaptor<WebSocketMessageDTO.SubscriptionResponse> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.SubscriptionResponse.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        WebSocketMessageDTO.SubscriptionResponse message = messageCaptor.getValue();
        assertThat(message.type()).isEqualTo(WebSocketMessageType.SUBSCRIBE);
        assertThat(message.chatRoomId()).isEqualTo(20L);
        assertThat(message.message()).isEqualTo("채팅방 구독이 완료되었습니다.");
        assertThat(message.timestamp()).isNotNull();
        then(sessionMessageSender).should().send(session, encodedMessage);
    }

    @Test
    @DisplayName("기존 채팅방 목록 구독 ACK의 type과 대상 방 목록을 유지한다")
    void sendChatListSubscriptionMessage_preservesLegacySubscribeAcknowledgement() {
        EncodedChatMessage encodedMessage = new EncodedChatMessage("chat-list-subscribe-json");
        given(session.isOpen()).willReturn(true);
        given(messageEncoder.encode(any(WebSocketMessageDTO.ChatListSubscriptionResponse.class)))
                .willReturn(Optional.of(encodedMessage));

        responseSender.sendChatListSubscriptionMessage(
                session, List.of(20L, 30L), "SUBSCRIBE_CHAT_LIST");

        ArgumentCaptor<WebSocketMessageDTO.ChatListSubscriptionResponse> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.ChatListSubscriptionResponse.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        WebSocketMessageDTO.ChatListSubscriptionResponse message = messageCaptor.getValue();
        assertThat(message.type()).isEqualTo(WebSocketMessageType.SUBSCRIBE_CHAT_LIST);
        assertThat(message.chatRoomIds()).containsExactly(20L, 30L);
        assertThat(message.message()).isEqualTo("채팅방 목록 구독이 완료되었습니다. (총 2개)");
        assertThat(message.timestamp()).isNotNull();
        then(sessionMessageSender).should().send(session, encodedMessage);
    }

    @Test
    @DisplayName("닫힌 세션에는 에러 응답을 인코딩하지 않는다")
    void sendErrorMessage_doesNotEncodeWhenSessionClosed() {
        // given
        given(session.isOpen()).willReturn(false);

        // when
        responseSender.sendErrorMessage(session, "ERROR", "오류");

        // then
        then(messageEncoder).shouldHaveNoInteractions();
        then(sessionMessageSender).shouldHaveNoInteractions();
    }
}
