package umc.cockple.demo.domain.chat.service.websocket.send.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SentMessageReadStatusService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public int markActiveSubscribersAsRead(Long chatRoomId, Long messageId, List<Long> activeSubscribers, Long senderId) {
        log.info("초기 읽음 처리 - 메시지: {}, 활성 구독자 수: {}, 발신자: {}",
                messageId, activeSubscribers.size(), senderId);

        List<Long> readers = activeSubscribers.stream()
                .filter(memberId -> !memberId.equals(senderId))
                .toList();

        if (!readers.isEmpty()) {
            int updatedCount = messageReadStatusRepository.markAsReadInMembers(messageId, readers);
            log.info("초기 읽음 처리 완료 - 처리된 구독자: {}명", updatedCount);

            int lastReadUpdatedCount = chatRoomMemberRepository.advanceLastReadMessageIdForMembers(
                    chatRoomId, readers, messageId);
            log.debug("활성 구독자 lastReadMessageId 배치 업데이트 완료 - 처리된 멤버: {}명", lastReadUpdatedCount);
        }

        int finalUnreadCount = messageReadStatusRepository.countUnreadByMessageId(messageId);
        log.info("초기 처리 후 최종 안읽은 수: {}", finalUnreadCount);

        return finalUnreadCount;
    }
}
