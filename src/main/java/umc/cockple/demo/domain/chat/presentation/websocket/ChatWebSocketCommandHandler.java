package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.events.ChatListSubscriptionEvent;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.service.websocket.validation.ChatWebSocketRequestValidator;
import umc.cockple.demo.domain.chat.service.websocket.command.ChatCommandResponder;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketCommandHandler {

    private final ChatWebSocketRequestValidator chatWebSocketRequestValidator;
    private final ApplicationEventPublisher eventPublisher;

    public void handle(
            WebSocketMessageDTO.Request request,
            Long memberId,
            ChatCommandResponder responder
    ) {
        switch (request.type()) {
            case SEND:
                handleSendMessage(request, memberId, responder);
                break;
            case SUBSCRIBE:
                handleSubscribe(request, memberId, responder);
                break;
            case UNSUBSCRIBE:
                handleUnsubscribe(request, memberId, responder);
                break;
            case SUBSCRIBE_CHAT_LIST:
                handleSubscribeChatList(request, memberId, responder);
                break;
            case UNSUBSCRIBE_CHAT_LIST:
                handleUnsubscribeChatList(request, memberId, responder);
                break;
            default:
                responder.sendError("UNKNOWN_TYPE", "알 수 없는 메시지 타입입니다:" + request.type());
        }
    }

    private void handleSendMessage(
            WebSocketMessageDTO.Request request,
            Long memberId,
            ChatCommandResponder responder
    ) {
        try {
            chatWebSocketRequestValidator.validateSendRequest(
                    request.chatRoomId(), request.content(), request.images(), memberId);

            ChatMessageSendEvent sendEvent =
                    ChatMessageSendEvent.create(
                            request.chatRoomId(), request.content(), request.images(), memberId);
            eventPublisher.publishEvent(sendEvent);

        } catch (ChatException e) {
            log.warn("메시지 전송 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getErrorReason().getMessage());
            responder.sendError(e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("메시지 전송 처리 중 예외 발생", e);
            responder.sendError("SEND_MESSAGE_ERROR", "메시지 전송 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleSubscribe(
            WebSocketMessageDTO.Request request,
            Long memberId,
            ChatCommandResponder responder
    ) {
        try {
            chatWebSocketRequestValidator.validateSubscriptionRequest(request.chatRoomId(), memberId);

            ChatRoomSubscriptionEvent subscribeEvent =
                    ChatRoomSubscriptionEvent.subscribe(request.chatRoomId(), memberId);
            eventPublisher.publishEvent(subscribeEvent);

            // ACK는 요청 검증과 구독 이벤트 접수 성공을 의미한다.
            // Redis 구독 저장, 읽음 처리, unread-count 브로드캐스트는 listener가 best-effort로 후속 처리한다.
            responder.acknowledgeRoomSubscription(request.chatRoomId(), "SUBSCRIBE");

        } catch (ChatException e) {
            log.warn("구독 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getErrorReason().getMessage());
            responder.sendError(e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("구독 처리 중 예외 발생", e);
            responder.sendError("SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleUnsubscribe(
            WebSocketMessageDTO.Request request,
            Long memberId,
            ChatCommandResponder responder
    ) {
        try {
            chatWebSocketRequestValidator.validateUnsubscriptionRequest(request.chatRoomId(), memberId);

            ChatRoomSubscriptionEvent unsubscribeEvent =
                    ChatRoomSubscriptionEvent.unsubscribe(request.chatRoomId(), memberId);
            eventPublisher.publishEvent(unsubscribeEvent);

            // ACK는 요청 검증과 구독 해제 이벤트 접수 성공을 의미한다.
            // Redis 구독 해제는 listener가 best-effort로 후속 처리하고 실패 시 error log만 남긴다.
            responder.acknowledgeRoomSubscription(request.chatRoomId(), "UNSUBSCRIBE");

        } catch (ChatException e) {
            log.warn("구독 해제 실패 - 채팅방: {}, 멤버: {}, 이유: {}", request.chatRoomId(), memberId, e.getErrorReason().getMessage());
            responder.sendError(e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("구독 해제 처리 중 예외 발생", e);
            responder.sendError("UNSUBSCRIPTION_ERROR", "구독 해제 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleSubscribeChatList(
            WebSocketMessageDTO.Request request,
            Long memberId,
            ChatCommandResponder responder
    ) {
        try {
            chatWebSocketRequestValidator.validateChatListSubscriptionRequest(memberId, request.memberRooms());

            ChatListSubscriptionEvent subscribeEvent =
                    ChatListSubscriptionEvent.subscribe(memberId, request.memberRooms());
            eventPublisher.publishEvent(subscribeEvent);

            // ACK는 요청 검증과 채팅방 목록 구독 이벤트 접수 성공을 의미한다.
            // Redis 목록 구독 반영은 비동기 listener가 best-effort로 후속 처리한다.
            responder.acknowledgeChatListSubscription(request.memberRooms(), "SUBSCRIBE_CHAT_LIST");

        } catch (ChatException e) {
            log.warn("채팅방 목록 구독 검증 실패 - 멤버: {}, 이유: {}", memberId, e.getErrorReason().getMessage());
            responder.sendError(e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("채팅방 목록 구독 처리 중 예외 발생", e);
            responder.sendError("SUBSCRIPTION_ERROR", "채팅방 목록 구독 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleUnsubscribeChatList(
            WebSocketMessageDTO.Request request,
            Long memberId,
            ChatCommandResponder responder
    ) {
        try {
            chatWebSocketRequestValidator.validateChatListUnsubscriptionRequest(memberId, request.memberRooms());

            ChatListSubscriptionEvent unsubscribeEvent =
                    ChatListSubscriptionEvent.unsubscribe(memberId, request.memberRooms());
            eventPublisher.publishEvent(unsubscribeEvent);

            // ACK는 요청 검증과 채팅방 목록 구독 해제 이벤트 접수 성공을 의미한다.
            // Redis 목록 구독 해제 반영은 비동기 listener가 best-effort로 후속 처리한다.
            responder.acknowledgeChatListSubscription(request.memberRooms(), "UNSUBSCRIBE_CHAT_LIST");

        } catch (ChatException e) {
            log.warn("채팅방 목록 구독 해제 검증 실패 - 멤버: {}, 이유: {}", memberId, e.getErrorReason().getMessage());
            responder.sendError(e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        } catch (Exception e) {
            log.error("채팅방 목록 구독 해제 처리 중 예외 발생", e);
            responder.sendError("UNSUBSCRIPTION_ERROR", "채팅방 목록 구독 해제 처리 중 오류가 발생했습니다.");
        }
    }
}
