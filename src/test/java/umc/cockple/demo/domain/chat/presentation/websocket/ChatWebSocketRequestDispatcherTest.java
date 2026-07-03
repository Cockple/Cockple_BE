package umc.cockple.demo.domain.chat.presentation.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketRequestDispatcher")
class ChatWebSocketRequestDispatcherTest {

    @Mock private WebSocketResponseSender webSocketResponseSender;
    @Mock private ChatWebSocketCommandHandler commandHandler;
    @Mock private WebSocketSession session;

    private ChatWebSocketRequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ChatWebSocketRequestDispatcher(
                new ObjectMapper(),
                webSocketResponseSender,
                commandHandler
        );
    }

    @Test
    @DisplayName("인증된 요청을 파싱해 command handler에 위임한다")
    void dispatch_delegatesAuthenticatedRequestToCommandHandler() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", memberId);
        given(session.getAttributes()).willReturn(attributes);
        given(session.getId()).willReturn("session-1");

        String payload = """
                {"type":"SEND","chatRoomId":20,"content":"hello","images":[]}
                """;

        // when
        dispatcher.dispatch(session, payload);

        // then
        ArgumentCaptor<WebSocketMessageDTO.Request> requestCaptor = ArgumentCaptor.forClass(WebSocketMessageDTO.Request.class);
        then(commandHandler).should().handle(eq(session), requestCaptor.capture(), eq(memberId));

        WebSocketMessageDTO.Request request = requestCaptor.getValue();
        assertThat(request.type()).isEqualTo(WebSocketMessageType.SEND);
        assertThat(request.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(request.content()).isEqualTo("hello");
        assertThat(request.images()).isEmpty();

        then(webSocketResponseSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("memberId가 없으면 인증 오류 응답을 전송하고 라우팅하지 않는다")
    void dispatch_sendsUnauthorizedError_whenMemberIdMissing() {
        // given
        given(session.getAttributes()).willReturn(new HashMap<>());
        String payload = """
                {"type":"SEND","chatRoomId":20,"content":"hello","images":[]}
                """;

        // when
        dispatcher.dispatch(session, payload);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
        then(commandHandler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("파싱 실패 시 내부 예외 메시지 없이 일반 처리 오류 응답을 전송한다")
    void dispatch_sendsGenericProcessingError_whenPayloadCannotBeParsed() {
        // given
        given(session.getId()).willReturn("session-1");
        String invalidPayload = "{invalid-json";

        // when
        dispatcher.dispatch(session, invalidPayload);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다.");
        then(commandHandler).shouldHaveNoInteractions();
    }

}
