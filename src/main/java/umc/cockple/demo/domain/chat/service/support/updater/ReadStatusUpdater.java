package umc.cockple.demo.domain.chat.service.support.updater;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.support.ReadStatusBatchSupport;

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

        int updatedCount = 0;
        for (List<Long> chunk : ReadStatusBatchSupport.chunk(messageIds)) {
            updatedCount += messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, chunk);
        }

        return updatedCount;
    }

    public int markMessageAsReadForMembers(Long messageId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (List<Long> chunk : ReadStatusBatchSupport.chunk(memberIds)) {
            updatedCount += messageReadStatusRepository.markAsReadInMembers(messageId, chunk);
        }

        return updatedCount;
    }
}
