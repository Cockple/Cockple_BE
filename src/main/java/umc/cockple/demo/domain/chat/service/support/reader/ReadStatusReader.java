package umc.cockple.demo.domain.chat.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.support.ReadStatusBatchSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReadStatusReader {

    private final MessageReadStatusRepository messageReadStatusRepository;

    public List<Long> findUnreadMessageIds(Long chatRoomId, Long memberId) {
        return messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);
    }

    public Map<Long, Integer> countUnreadByMessageIdsAsSparseMap(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> unreadCounts = new HashMap<>();
        for (List<Long> chunk : ReadStatusBatchSupport.chunk(messageIds)) {
            messageReadStatusRepository.countUnreadByMessageIds(chunk)
                    .forEach(count -> unreadCounts.put(
                            count.chatMessageId(),
                            count.unreadCount().intValue()
                    ));
        }

        return unreadCounts;
    }

    public int countUnreadByMessageId(Long messageId) {
        return messageReadStatusRepository.countUnreadByMessageId(messageId);
    }
}
