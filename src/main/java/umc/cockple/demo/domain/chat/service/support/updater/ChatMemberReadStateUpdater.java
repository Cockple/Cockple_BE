package umc.cockple.demo.domain.chat.service.support.updater;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatMemberReadStateUpdater {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public int advanceLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        int updatedCount = chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, messageId);
        if (updatedCount == 0) {
            log.warn(
                    "lastReadMessageId 미갱신 - 채팅방: {}, 멤버: {}, 메시지: {} (멤버십 없음 또는 이미 최신 상태)",
                    chatRoomId,
                    memberId,
                    messageId
            );
        }

        return updatedCount;
    }

    public int advanceLastReadMessageIdForMembers(Long chatRoomId, List<Long> memberIds, Long messageId) {
        if (memberIds.isEmpty()) {
            return 0;
        }

        int updatedCount = chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId);
        if (updatedCount < memberIds.size()) {
            log.warn(
                    "lastReadMessageId 일부 미갱신 - 채팅방: {}, 요청 멤버 수: {}, 갱신 수: {}, 메시지: {} "
                            + "(멤버십 없음 또는 이미 최신 상태 포함 가능)",
                    chatRoomId,
                    memberIds.size(),
                    updatedCount,
                    messageId
            );
        }

        return updatedCount;
    }
}
