package umc.cockple.demo.domain.chat.service.websocket.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.UnreadCountUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.ActiveChatRoomSubscriberReader;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomSubscriptionService {

    /*
     * 이 서비스는 이미 접수된 WebSocket 구독 명령의 후속 side effect를 수행한다.
     *
     * 일관성 수준은 best-effort다. 읽음 처리나 unread-count 브로드캐스트가 실패해도 listener가 실패를 error log로
     * 남기고 socket ACK 경로로 전파하지 않는다. Redis 구독 상태 변경을 먼저 시도하며, 현재 계약에는 재시도/보상이 없다.
     */

    private final SubscribeReadStatusService subscribeReadStatusService;
    private final ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    private final UnreadCountUpdateBroadcaster unreadCountUpdateBroadcaster;
    private final ActiveChatRoomSubscriberReader activeChatRoomSubscriberReader;

    public void subscribeToChatRoom(Long chatRoomId, Long memberId) {
        chatRoomSubscriptionStore.addSubscriber(chatRoomId, memberId);
        log.info("채팅방 구독 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);

        List<SubscribeReadStatusService.MessageUnreadUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        if (!updates.isEmpty()) {
            List<Long> subscribers = activeChatRoomSubscriberReader.findActiveSubscribers(chatRoomId);
            unreadCountUpdateBroadcaster.broadcast(chatRoomId, updates, subscribers, memberId);
            log.info("구독으로 인한 안읽은 수 업데이트 브로드캐스트 완료 - 업데이트된 메시지 수: {}", updates.size());
        }
    }

    public void unsubscribeToChatRoom(Long chatRoomId, Long memberId) {
        chatRoomSubscriptionStore.removeSubscriber(chatRoomId, memberId);
        log.info("채팅방 구독 해제 완료 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);
    }
}
