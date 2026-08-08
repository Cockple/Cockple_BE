package umc.cockple.demo.domain.chat.presentation.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.presentation.websocket.session.WebSocketSessionRegistry;
import umc.cockple.demo.domain.member.service.MemberQueryService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketHandler")
class ChatWebSocketHandlerTest {

    @Mock private MemberQueryService memberQueryService;
    @Mock private WebSocketResponseSender webSocketResponseSender;
    @Mock private ChatWebSocketRequestDispatcher requestDispatcher;
    @Mock private WebSocketSessionRegistry sessionRegistry;
    @Mock private WebSocketSession session;

    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(
                memberQueryService,
                webSocketResponseSender,
                requestDispatcher,
                sessionRegistry
        );
    }

    @Test
    @DisplayName("인증된 연결은 회원 정보를 조회해 세션을 등록하고 기존 CONNECT 응답을 보낸다")
    void afterConnectionEstablishedRegistersAuthenticatedMemberAndSendsConnectionResponse() throws Exception {
        Long memberId = 10L;
        MemberConnectionInfo memberInfo = new MemberConnectionInfo(memberId, "홍길동");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", memberId);
        attributes.put("authenticated", true);
        given(session.getAttributes()).willReturn(attributes);
        given(session.getId()).willReturn("session-1");
        given(memberQueryService.getMemberConnectionInfo(memberId)).willReturn(memberInfo);

        handler.afterConnectionEstablished(session);

        then(memberQueryService).should().getMemberConnectionInfo(memberId);
        then(sessionRegistry).should().register(memberId, session);
        then(webSocketResponseSender).should().sendConnectionSuccessMessage(session, memberInfo);
        then(session).should(never()).close();
        assertThat(attributes).containsEntry("memberName", "홍길동");
    }

    @Test
    @DisplayName("인증 정보가 없는 연결은 등록하지 않고 세션을 종료한다")
    void afterConnectionEstablishedClosesUnauthenticatedSession() throws Exception {
        given(session.getAttributes()).willReturn(new HashMap<>());

        handler.afterConnectionEstablished(session);

        then(session).should().close();
        then(memberQueryService).shouldHaveNoInteractions();
        then(sessionRegistry).shouldHaveNoInteractions();
        then(webSocketResponseSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("연결 처리 중 회원 조회가 실패하면 세션을 등록하지 않고 종료한다")
    void afterConnectionEstablishedClosesSessionWhenMemberLookupFails() throws Exception {
        Long memberId = 10L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", memberId);
        attributes.put("authenticated", true);
        given(session.getAttributes()).willReturn(attributes);
        willThrow(new IllegalStateException("lookup failed"))
                .given(memberQueryService)
                .getMemberConnectionInfo(memberId);

        handler.afterConnectionEstablished(session);

        then(session).should().close();
        then(sessionRegistry).shouldHaveNoInteractions();
        then(webSocketResponseSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("수신한 text payload를 기존 dispatcher에 그대로 전달한다")
    void handleTextMessageDelegatesPayloadToDispatcher() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", 10L);
        given(session.getAttributes()).willReturn(attributes);
        given(session.getId()).willReturn("session-1");
        TextMessage message = new TextMessage("{\"type\":\"SEND\"}");

        handler.handleTextMessage(session, message);

        then(requestDispatcher).should().dispatch(session, message.getPayload());
    }

    @Test
    @DisplayName("연결 종료 시 닫힌 세션과 일치할 때만 세션 저장소에서 제거한다")
    void afterConnectionClosed_removesClosedSessionByIdentity() {
        // given
        Long memberId = 10L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", memberId);
        given(session.getAttributes()).willReturn(attributes);
        given(session.getId()).willReturn("session-1");

        // when
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // then
        then(sessionRegistry).should().remove(memberId, session);
    }
}
