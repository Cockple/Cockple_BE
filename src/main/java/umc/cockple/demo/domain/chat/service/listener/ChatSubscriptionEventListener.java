package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.events.ChatListSubscriptionEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.subscription.ChatRoomSubscriptionService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSubscriptionEventListener {

    /*
     * 구독 이벤트 후속 처리는 best-effort 계약이다.
     *
     * WebSocket command handler는 요청 검증과 이벤트 발행이 끝나면 ACK를 보낸다. 이 listener는 Redis 구독 상태,
     * 읽음 처리, unread-count 브로드캐스트 실패를 error log로 남기고 삼킨다. 현재 재시도나 보상 정책은 없으므로,
     * ACK를 모든 후속 side effect 완료 보장으로 해석하면 안 된다.
     */

    private final ChatRoomSubscriptionService chatRoomSubscriptionService;
    private final ChatListSubscriptionStore chatListSubscriptionStore;

    @EventListener
    public void handleChatRoomSubscription(ChatRoomSubscriptionEvent event) {
        log.info("채팅방 구독 이벤트 처리 - 채팅방: {}, 사용자: {}, 액션: {}",
                event.chatRoomId(), event.memberId(), event.action());

        try {
            switch (event.action()) {
                case "SUBSCRIBE" -> {
                    chatRoomSubscriptionService.subscribeToChatRoom(event.chatRoomId(), event.memberId());
                    log.info("사용자 {}가 채팅방 {}를 구독했습니다.", event.memberId(), event.chatRoomId());
                }
                case "UNSUBSCRIBE" -> {
                    chatRoomSubscriptionService.unsubscribeToChatRoom(event.chatRoomId(), event.memberId());
                    log.info("사용자 {}가 채팅방 {}를 구독해제했습니다.", event.memberId(), event.chatRoomId());
                }
                default -> log.warn("알 수 없는 구독 액션: {}", event.action());
            }
        } catch (Exception e) {
            log.error("채팅방 구독 이벤트 처리 중 오류 발생", e);
        }
    }

    @EventListener
    @Async
    public void handleChatListSubscription(ChatListSubscriptionEvent event) {
        log.info("채팅방 목록 구독 이벤트 처리 시작 - 멤버: {}, 액션: {}, 채팅방 수: {}",
                event.memberId(), event.action(), event.chatRoomIds().size());

        try {
            switch (event.action()) {
                case "SUBSCRIBE" -> {
                    chatListSubscriptionStore.subscribeToChatList(event.memberId(), event.chatRoomIds());
                    log.info("채팅방 목록 구독 완료 - 멤버: {}, 채팅방 수: {}", event.memberId(), event.chatRoomIds().size());
                }
                case "UNSUBSCRIBE" -> {
                    chatListSubscriptionStore.unsubscribeFromChatList(event.memberId(), event.chatRoomIds());
                    log.info("채팅방 목록 구독 해제 완료 - 멤버: {}, 채팅방 수: {}", event.memberId(), event.chatRoomIds().size());
                }
                default -> log.warn("알 수 없는 채팅방 목록 구독 액션: {}", event.action());
            }

        } catch (Exception e) {
            log.error("채팅방 목록 구독 이벤트 처리 중 오류 발생 - 멤버: {}, 액션: {}",
                    event.memberId(), event.action(), e);
        }
    }
}
