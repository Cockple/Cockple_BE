package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

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

    public List<MessageUnreadUpdate> markUnreadMessagesAsReadOnSubscribe(Long chatRoomId, Long memberId) {
        log.info("구독 시 안읽은 메시지 처리 시작 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        List<Long> unreadMessageIds = messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);

        if (unreadMessageIds.isEmpty()) {
            log.debug("처리할 안읽은 메시지가 없음 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);
            return List.of();
        }
        log.debug("처리할 안읽은 메시지 수: {} - 채팅방: {}, 멤버: {}", unreadMessageIds.size(), chatRoomId, memberId);

        int processedCount = messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, unreadMessageIds);
        log.debug("구독 시 메시지 읽음 배치 처리 완료 - 처리된 메시지 수: {}", processedCount);

        Map<Long, Integer> unreadCounts = messageReadStatusRepository.countUnreadByMessageIds(unreadMessageIds)
                .stream()
                .collect(Collectors.toMap(
                        count -> count.chatMessageId(),
                        count -> count.unreadCount().intValue()
                ));

        List<MessageUnreadUpdate> updates = unreadMessageIds.stream()
                .map(messageId -> new MessageUnreadUpdate(messageId, unreadCounts.getOrDefault(messageId, 0)))
                .toList();

        if (!unreadMessageIds.isEmpty()) {
            Long latestMessageId = unreadMessageIds.get(unreadMessageIds.size() - 1);
            updateLastReadMessageId(chatRoomId, memberId, latestMessageId);
            log.debug("lastReadMessageId 업데이트 완료 - 채팅방: {}, 멤버: {}, 최신 메시지: {}",
                    chatRoomId, memberId, latestMessageId);
        }

        log.info("구독 시 안읽은 메시지 처리 완료 - 채팅방: {}, 멤버: {}, 처리된 메시지 수: {}",
                chatRoomId, memberId, updates.size());

        eventPublisher.publishEvent(ChatUnreadStatusUpdateEvent.of(List.of(memberId)));
        log.info("구독 읽음 처리 후 안읽음 상태 업데이트 이벤트 발행 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        return updates;
    }

    private void updateLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        try {
            int updatedCount = chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, messageId);
            log.debug("구독 시 lastReadMessageId 배치 업데이트 - 채팅방: {}, 멤버: {}, 메시지: {}, 처리 수: {}",
                    chatRoomId, memberId, messageId, updatedCount);
        } catch (Exception e) {
            log.error("구독 시 lastReadMessageId 업데이트 실패 - 멤버: {}, 메시지: {}", memberId, messageId, e);
        }
    }

    public record MessageUnreadUpdate(
            Long messageId,
            int newUnreadCount
    ) {
    }
}
