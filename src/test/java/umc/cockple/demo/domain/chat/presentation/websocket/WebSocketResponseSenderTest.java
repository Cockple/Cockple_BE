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
