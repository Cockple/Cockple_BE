package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReadProcessingService {

    private final MessageReadStatusRepository messageReadStatusRepository;

    public List<MessageUnreadUpdate> processUnreadMessagesOnSubscribe(Long chatRoomId, Long memberId) {
        log.info("구독 시 안읽은 메시지 처리 시작 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        List<Long> unreadMessageIds = messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);

        if (unreadMessageIds.isEmpty()) {
            log.debug("처리할 안읽은 메시지가 없음 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);
            return List.of();
        }
        log.debug("처리할 안읽은 메시지 수: {} - 채팅방: {}, 멤버: {}", unreadMessageIds.size(), chatRoomId, memberId);

        List<MessageUnreadUpdate> updates = unreadMessageIds.stream()
                .map(messageId -> {
                    log.debug("메시지 읽음 처리 중 - 메시지: {}, 멤버: {}", messageId, memberId);
                    int processedCount = messageReadStatusRepository.markAsReadInMembers(messageId, List.of(memberId));
                    int newUnreadCount = messageReadStatusRepository.countUnreadByMessageId(messageId);
                    log.debug("메시지 읽음 처리 완료 - 메시지: {}, 처리 결과: {}, 새 안읽은 수: {}",
                            messageId, processedCount > 0 ? "성공" : "이미 읽음", newUnreadCount);

                    return new MessageUnreadUpdate(messageId, newUnreadCount);
                })
                .toList();


    }

    public record MessageUnreadUpdate(
            Long messageId,
            int newUnreadCount
    ) {}
}
