package umc.cockple.demo.domain.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.chat.service.SubscriptionService;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SubscriptionService subscriptionService;
    private final ChatWebSocketService chatWebSocketService;
    private final ChatValidator chatValidator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("웹소켓 연결 성공");

        try {
            Long memberId = (Long) session.getAttributes().get("memberId");
            Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");

            if (memberId != null && Boolean.TRUE.equals(authenticated)) {
                MemberConnectionInfo memberInfo = chatWebSocketService.getMemberConnectionInfo(memberId);
                session.getAttributes().put("memberName", memberInfo.memberName());

                subscriptionService.addSession(memberId, session);
                log.info("사용자 연결 완료 - memberId: {}, 세션 ID: {}", memberId, session.getId());

                sendConnectionSuccessMessage(session, memberInfo);
            } else {
                log.warn("memberId를 찾을 수 없습니다. 세션을 종료합니다.");
                session.close();
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생", e);
            session.close();
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("메시지 수신");
        log.info("메시지: {}", message.getPayload());

        try {
            WebSocketMessageDTO.Request request = objectMapper.readValue(
                    message.getPayload(), WebSocketMessageDTO.Request.class
            );

            Long memberId = (Long) session.getAttributes().get("memberId");
            if (memberId == null) {
                sendErrorMessage(session, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
                return;
            }

            log.info("메시지 타입: {}, 채팅방 ID: {}, 사용자 ID: {}", memberId, session.getId(), memberId);

            switch (request.type()) {
                case SEND:
                    ChatMessageSendEvent sendEvent =
                            ChatMessageSendEvent.create(
                                    request.chatRoomId(), request.content(), request.files(), request.images(), memberId);
                    eventPublisher.publishEvent(sendEvent);
                    break;
                case SUBSCRIBE:
                    handleSubscribe(session, request, memberId);
                    break;
                case UNSUBSCRIBE:
                    handleUnsubscribe(session,request,memberId);
                    break;
                default:
                    sendErrorMessage(session, "UNKNOWN_TYPE", "알 수 없는 메시지 타입입니다:" + request.type());
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생", e);
            sendErrorMessage(session, "PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다:" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");

        log.info("웹소켓 연결 종료");
        log.info("세션 ID: {}, 사용자 ID: {}, 종료 상태: {}", session.getId(), memberId, status);

        if (memberId != null) {
            subscriptionService.removeSession(memberId);
            log.info("사용자 세션 정리 완료 - memberId: {}", memberId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");
        log.error("WebSocket 전송 오류 발생 - 세션 ID: {}, 사용자 ID: {}", session.getId(), memberId, exception);
    }

    // ========== 내부 메서드들 ==========

    private void handleSubscribe(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateSubscriptionRequest(request.chatRoomId(), memberId);

            ChatRoomSubscriptionEvent subscribeEvent =
                    ChatRoomSubscriptionEvent.subscribe(request.chatRoomId(), memberId);
            eventPublisher.publishEvent(subscribeEvent);

            sendSubscriptionMessage(session, request.chatRoomId(), "SUBSCRIBE");

        } catch (ChatException e) {
            log.warn("구독 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getMessage());
            sendErrorMessage(session, e.getErrorReason().getCode(), e.getMessage(), request.chatRoomId());
        } catch (Exception e) {
            log.error("구독 처리 중 예외 발생", e);
            sendErrorMessage(session, "SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다.", request.chatRoomId());
        }
    }

    private void handleUnsubscribe(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateUnsubscriptionRequest(request.chatRoomId(), memberId);

            ChatRoomSubscriptionEvent unsubscribeEvent =
                    ChatRoomSubscriptionEvent.unsubscribe(request.chatRoomId(), memberId);
            eventPublisher.publishEvent(unsubscribeEvent);

            sendSubscriptionMessage(session, request.chatRoomId(), "UNSUBSCRIBE");

        } catch (ChatException e) {
            log.warn("구독 해제 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getMessage());
            sendErrorMessage(session, e.getErrorReason().getCode(), e.getMessage(), request.chatRoomId());
        } catch (Exception e) {
            log.error("구독 해제 처리 중 예외 발생", e);
            sendErrorMessage(session, "UNSUBSCRIPTION_ERROR", "구독 해제 처리 중 오류가 발생했습니다.", request.chatRoomId());
        }
    }

    // ========== 반환 메시지 생성 메서드 ============

    private void sendConnectionSuccessMessage(WebSocketSession session, MemberConnectionInfo memberInfo) {
        try {
            WebSocketMessageDTO.ConnectionInfo connectionInfo = WebSocketMessageDTO.ConnectionInfo.builder()
                    .type(WebSocketMessageType.CONNECT)
                    .memberId(memberInfo.memberId())
                    .memberName(memberInfo.memberName())
                    .timestamp(LocalDateTime.now())
                    .message("WebSocket 연결이 성공했습니다.")
                    .build();

            String messageJson = objectMapper.writeValueAsString(connectionInfo);
            session.sendMessage(new TextMessage(messageJson));

            log.info("연결 성공 메시지 전송 완료 - memberId: {}", memberInfo.memberId());
        } catch (Exception e) {
            log.error("연결 성공 메시지 전송 실패", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorCode, String message) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 에러 메시지를 전송할 수 없습니다.");
            return;
        }

        try {
            WebSocketMessageDTO.ErrorResponse errorResponse = WebSocketMessageDTO.ErrorResponse.builder()
                    .type(WebSocketMessageType.ERROR)
                    .errorCode(errorCode)
                    .message(message)
                    .build();

            String errorJson = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(errorJson));

            log.info("에러 메시지 전송 완료 - 코드: {}, 메시지: {}", errorCode, message);
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패", e);
        }
    }

    private void sendSubscriptionMessage(WebSocketSession session, Long chatRoomId, String action) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 구독 응답 메시지를 전송할 수 없습니다.");
            return;
        }

        try {
            WebSocketMessageDTO.SubscriptionResponse subscriptionResponse = WebSocketMessageDTO.SubscriptionResponse.builder()
                    .type(WebSocketMessageType.valueOf(action))
                    .chatRoomId(chatRoomId)
                    .message(action.equals("SUBSCRIBE") ? "채팅방 구독이 완료되었습니다." : "채팅방 구독이 해제되었습니다.")
                    .timestamp(LocalDateTime.now())
                    .build();

            String responseJson = objectMapper.writeValueAsString(subscriptionResponse);
            session.sendMessage(new TextMessage(responseJson));

            log.info("구독 응답 메시지 전송 완료 - 액션: {}, 채팅방: {}", action, chatRoomId);
        } catch (Exception e) {
            log.error("구독 응답 메시지 전송 실패", e);
        }
    }
}
