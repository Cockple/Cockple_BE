package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.service.support.reader.ReadStatusReader;
import umc.cockple.demo.domain.chat.service.support.updater.ChatMemberReadStateUpdater;
import umc.cockple.demo.domain.chat.service.support.updater.ReadStatusUpdater;
import umc.cockple.demo.domain.chat.service.websocket.UnreadCountUpdate;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscribeReadStatusService {

    private final ReadStatusReader readStatusReader;
    private final ReadStatusUpdater readStatusUpdater;
    private final ChatMemberReadStateUpdater chatMemberReadStateUpdater;
    private final ApplicationEventPublisher eventPublisher;

    public List<UnreadCountUpdate> markUnreadMessagesAsReadOnSubscribe(Long chatRoomId, Long memberId) {
        List<Long> unreadMessageIds = readStatusReader.findUnreadMessageIds(chatRoomId, memberId);
        if (unreadMessageIds.isEmpty()) {
            return List.of();
        }

        readStatusUpdater.markMessagesAsReadForMember(chatRoomId, memberId, unreadMessageIds);

        List<UnreadCountUpdate> updates = createUnreadUpdates(unreadMessageIds);
        updateLastReadMessageId(chatRoomId, memberId, latestMessageId(unreadMessageIds));
        eventPublisher.publishEvent(ChatUnreadStatusUpdateEvent.of(List.of(memberId)));

        return updates;
    }

    private List<UnreadCountUpdate> createUnreadUpdates(List<Long> unreadMessageIds) {
        Map<Long, Integer> unreadCounts = readStatusReader.countUnreadByMessageIds(unreadMessageIds);

        return unreadMessageIds.stream()
                .map(messageId -> new UnreadCountUpdate(messageId, unreadCounts.getOrDefault(messageId, 0)))
                .toList();
    }

    private Long latestMessageId(List<Long> messageIds) {
        return messageIds.get(messageIds.size() - 1);
    }

    private void updateLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        try {
            chatMemberReadStateUpdater.advanceLastReadMessageId(chatRoomId, memberId, messageId);
        } catch (Exception e) {
            log.error("구독 시 lastReadMessageId 업데이트 실패 - 멤버: {}, 메시지: {}", memberId, messageId, e);
        }
    }
}
