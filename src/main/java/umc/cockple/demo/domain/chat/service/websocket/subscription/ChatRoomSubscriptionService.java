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
