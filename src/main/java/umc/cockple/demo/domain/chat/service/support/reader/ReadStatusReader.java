package umc.cockple.demo.domain.chat.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatMessageUnreadCountDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReadStatusReader {

    private final MessageReadStatusRepository messageReadStatusRepository;

    public List<Long> findUnreadMessageIds(Long chatRoomId, Long memberId) {
        return messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);
    }

    public Map<Long, Integer> countUnreadByMessageIds(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        return messageReadStatusRepository.countUnreadByMessageIds(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        ChatMessageUnreadCountDTO::chatMessageId,
                        count -> count.unreadCount().intValue()
                ));
    }

    public int countUnreadByMessageId(Long messageId) {
        return messageReadStatusRepository.countUnreadByMessageId(messageId);
    }
}
