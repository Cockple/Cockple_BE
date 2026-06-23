package umc.cockple.demo.domain.chat.service.support.updater;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMemberReadStateUpdater {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public int advanceLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        return chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, messageId);
    }

    public int advanceLastReadMessageIdForMembers(Long chatRoomId, List<Long> memberIds, Long messageId) {
        if (memberIds.isEmpty()) {
            return 0;
        }

        return chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId);
    }
}
