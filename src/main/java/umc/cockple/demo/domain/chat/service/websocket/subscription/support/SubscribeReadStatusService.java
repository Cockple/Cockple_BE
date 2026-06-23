package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatMessageUnreadCountDTO;
import umc.cockple.demo.domain.chat.service.websocket.UnreadCountUpdate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscribeReadStatusService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<UnreadCountUpdate> markUnreadMessagesAsReadOnSubscribe(Long chatRoomId, Long memberId) {
        List<Long> unreadMessageIds = messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);
        if (unreadMessageIds.isEmpty()) {
            return List.of();
        }

        messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, unreadMessageIds);

        List<UnreadCountUpdate> updates = createUnreadUpdates(unreadMessageIds);
        updateLastReadMessageId(chatRoomId, memberId, latestMessageId(unreadMessageIds));
        eventPublisher.publishEvent(ChatUnreadStatusUpdateEvent.of(List.of(memberId)));

        return updates;
    }

    private List<UnreadCountUpdate> createUnreadUpdates(List<Long> unreadMessageIds) {
        Map<Long, Integer> unreadCounts = countUnreadMessages(unreadMessageIds);

        return unreadMessageIds.stream()
                .map(messageId -> new UnreadCountUpdate(messageId, unreadCounts.getOrDefault(messageId, 0)))
                .toList();
    }

    private Map<Long, Integer> countUnreadMessages(List<Long> messageIds) {
        return messageReadStatusRepository.countUnreadByMessageIds(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        ChatMessageUnreadCountDTO::chatMessageId,
                        count -> count.unreadCount().intValue()
                ));
    }

    private Long latestMessageId(List<Long> messageIds) {
        return messageIds.get(messageIds.size() - 1);
    }

    private void updateLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        try {
            chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, messageId);
        } catch (Exception e) {
            log.error("구독 시 lastReadMessageId 업데이트 실패 - 멤버: {}, 메시지: {}", memberId, messageId, e);
        }
    }
}
