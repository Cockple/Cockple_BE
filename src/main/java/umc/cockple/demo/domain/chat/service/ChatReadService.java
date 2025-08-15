package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatReadService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public int subscribersToReadStatus(Long chatRoomId, Long messageId, List<Long> activeSubscribers, Long senderId) {
        log.info("초기 읽음 처리 - 메시지: {}, 활성 구독자 수: {}, 발신자: {}",
                messageId, activeSubscribers.size(), senderId);

        List<Long> readers = activeSubscribers.stream()
                .filter(memberId -> !memberId.equals(senderId))
                .toList();

        if (!readers.isEmpty()) {
            int updatedCount = messageReadStatusRepository.markAsReadInMembers(messageId, readers);
            log.info("초기 읽음 처리 완료 - 처리된 구독자: {}명", updatedCount);

            updateLastReadMessageIds(chatRoomId, messageId, readers);
        }

        int finalUnreadCount = messageReadStatusRepository.countUnreadByMessageId(messageId);
        log.info("초기 처리 후 최종 안읽은 수: {}", finalUnreadCount);

        return finalUnreadCount;
    }

    private void updateLastReadMessageIds(Long chatRoomId, Long messageId, List<Long> memberIds) {
        for (Long memberId : memberIds) {
            try {
                Optional<ChatRoomMember> chatRoomMemberOpt =
                        chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);

                if (chatRoomMemberOpt.isPresent()) {
                    ChatRoomMember chatRoomMember = chatRoomMemberOpt.get();

                    if (chatRoomMember.getLastReadMessageId() == null ||
                            messageId > chatRoomMember.getLastReadMessageId()) {
                        chatRoomMember.updateLastReadMessageId(messageId);
                    }
                }
            } catch (Exception e) {
                log.error("lastReadMessageId 업데이트 실패 - 멤버: {}, 메시지: {}", memberId, messageId, e);
            }
        }
    }
}
