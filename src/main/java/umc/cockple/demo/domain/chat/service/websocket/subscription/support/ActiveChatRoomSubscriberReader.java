package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ActiveChatRoomSubscriberReader {

    private final ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    private final ChatSessionRegistry sessionRegistry;

    public List<Long> findActiveSubscribers(Long chatRoomId) {
        Set<Long> subscribedMemberIds = chatRoomSubscriptionStore.getSubscribers(chatRoomId);
        return sessionRegistry.findOpenMemberIds(subscribedMemberIds);
    }
}
