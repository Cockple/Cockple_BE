package umc.cockple.demo.domain.chat.service.support.updater;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReadStatusUpdater {

    private final MessageReadStatusRepository messageReadStatusRepository;

    public int markMessagesAsReadForMember(Long chatRoomId, Long memberId, List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return 0;
        }

        return messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, messageIds);
    }

    public int markMessageAsReadForMembers(Long messageId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return 0;
        }

        return messageReadStatusRepository.markAsReadInMembers(messageId, memberIds);
    }
}
