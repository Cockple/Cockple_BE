package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatReadService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsRead(Long chatRoomId, Long messageId, List<Long> memberIds) {
        log.debug("읽음 처리 시작 - 채팅방: {}, 메시지: {}, 멤버 수: {}", chatRoomId, messageId, memberIds.size());

        List<ChatRoomMember> chatRoomMembers = findChatRoomMembersInChatRoom(chatRoomId, memberIds);

        int updatedCount = 0;
        for (ChatRoomMember chatRoomMember : chatRoomMembers) {
            try {
                if (chatRoomMember.getLastReadMessageId() == null || messageId > chatRoomMember.getLastReadMessageId()) {
                    chatRoomMember.updateLastReadMessageId(messageId);
                    updatedCount++;
                    log.debug("읽음 처리 완료 - 멤버: {}", chatRoomMember.getMember().getId());
                }
            } catch (Exception e) {
                log.error("읽음 처리 실패 - 멤버: {}", chatRoomMember.getMember().getId(), e);
            }
        }
        log.info("전체 읽음 처리 완료 - 채팅방: {}, 업데이트된 멤버: {}명", chatRoomId, updatedCount);
    }

    private List<ChatRoomMember> findChatRoomMembersInChatRoom(Long chatRoomId, List<Long> memberIds) {
        return chatRoomMemberRepository.findChatRoomMembersInChatRoom(chatRoomId, memberIds);
    }
}
