package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.events.ChatListSubscriptionEvent;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.service.ChatValidator;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketCommandHandler {

    private final ChatValidator chatValidator;
    private final WebSocketResponseSender webSocketResponseSender;
    private final ApplicationEventPublisher eventPublisher;

    public void handle(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        switch (request.type()) {
            case SEND:
                handleSendMessage(session, request, memberId);
                break;
            case SUBSCRIBE:
                handleSubscribe(session, request, memberId);
                break;
            case UNSUBSCRIBE:
                handleUnsubscribe(session, request, memberId);
                break;
            case SUBSCRIBE_CHAT_LIST:
                handleSubscribeChatList(session, request, memberId);
                break;
            case UNSUBSCRIBE_CHAT_LIST:
                handleUnsubscribeChatList(session, request, memberId);
                break;
            default:
                webSocketResponseSender.sendErrorMessage(session, "UNKNOWN_TYPE", "알 수 없는 메시지 타입입니다:" + request.type());
        }
    }

    private void handleSendMessage(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateSendRequest(
                    request.chatRoomId(), request.content(), request.images(), memberId);

            ChatMessageSendEvent sendEvent =
                    ChatMessageSendEvent.create(
                            request.chatRoomId(), request.content(), request.images(), memberId);
            eventPublisher.publishEvent(sendEvent);

        } catch (ChatException e) {
            log.warn("메시지 전송 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getErrorReason().getMessage());
            webSocketResponseSender.sendErrorMessage(session, e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("메시지 전송 처리 중 예외 발생", e);
            webSocketResponseSender.sendErrorMessage(session, "SEND_MESSAGE_ERROR", "메시지 전송 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleSubscribe(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateSubscriptionRequest(request.chatRoomId(), memberId);

            ChatRoomSubscriptionEvent subscribeEvent =
                    ChatRoomSubscriptionEvent.subscribe(request.chatRoomId(), memberId);
            eventPublisher.publishEvent(subscribeEvent);

            webSocketResponseSender.sendSubscriptionMessage(session, request.chatRoomId(), "SUBSCRIBE");

        } catch (ChatException e) {
            log.warn("구독 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getErrorReason().getMessage());
            webSocketResponseSender.sendErrorMessage(session, e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("구독 처리 중 예외 발생", e);
            webSocketResponseSender.sendErrorMessage(session, "SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleUnsubscribe(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateUnsubscriptionRequest(request.chatRoomId(), memberId);

            ChatRoomSubscriptionEvent unsubscribeEvent =
                    ChatRoomSubscriptionEvent.unsubscribe(request.chatRoomId(), memberId);
            eventPublisher.publishEvent(unsubscribeEvent);

            webSocketResponseSender.sendSubscriptionMessage(session, request.chatRoomId(), "UNSUBSCRIBE");

        } catch (ChatException e) {
            log.warn("구독 해제 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getErrorReason().getMessage());
            webSocketResponseSender.sendErrorMessage(session, e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("구독 해제 처리 중 예외 발생", e);
            webSocketResponseSender.sendErrorMessage(session, "UNSUBSCRIPTION_ERROR", "구독 해제 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleSubscribeChatList(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateChatListSubscriptionRequest(memberId, request.memberRooms());

            ChatListSubscriptionEvent subscribeEvent =
                    ChatListSubscriptionEvent.subscribe(memberId, request.memberRooms());
            eventPublisher.publishEvent(subscribeEvent);

            webSocketResponseSender.sendChatListSubscriptionMessage(session, request.memberRooms(), "SUBSCRIBE_CHAT_LIST");

        } catch (ChatException e) {
            log.warn("채팅방 목록 구독 검증 실패 - 멤버: {}, 이유: {}", memberId, e.getErrorReason().getMessage());
            webSocketResponseSender.sendErrorMessage(session, e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("채팅방 목록 구독 처리 중 예외 발생", e);
            webSocketResponseSender.sendErrorMessage(session, "SUBSCRIPTION_ERROR", "채팅방 목록 구독 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleUnsubscribeChatList(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        try {
            chatValidator.validateChatListUnsubscriptionRequest(memberId, request.memberRooms());

            ChatListSubscriptionEvent unsubscribeEvent =
                    ChatListSubscriptionEvent.unsubscribe(memberId, request.memberRooms());
            eventPublisher.publishEvent(unsubscribeEvent);

            webSocketResponseSender.sendChatListSubscriptionMessage(session, request.memberRooms(), "UNSUBSCRIBE_CHAT_LIST");

        } catch (ChatException e) {
            log.warn("채팅방 목록 구독 해제 검증 실패 - 멤버: {}, 이유: {}", memberId, e.getErrorReason().getMessage());
            webSocketResponseSender.sendErrorMessage(session, e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("채팅방 목록 구독 해제 처리 중 예외 발생", e);
            webSocketResponseSender.sendErrorMessage(session, "UNSUBSCRIPTION_ERROR", "채팅방 목록 구독 해제 처리 중 오류가 발생했습니다.");
        }
    }
}
