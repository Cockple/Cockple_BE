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

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsRead(Long chatRoomId, Long messageId, List<Long> memberIds) {
        log.info("읽음 처리 시작 - 채팅방: {}, 메시지: {}, 멤버 수: {}", chatRoomId, messageId, memberIds.size());

        int updatedCount = messageReadStatusRepository.markAsReadInMembers(messageId, memberIds);

        for (Long memberId : memberIds) {
            updateLastReadMessageId(chatRoomId, memberId, messageId);
        }

        log.info("읽음 처리 완료 - 채팅방: {}, 업데이트된 상태: {}", messageId, updatedCount);
    }

    private void updateLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        Optional<ChatRoomMember> chatRoomMemberOpt =
                chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);

        if (chatRoomMemberOpt.isPresent()) {
            ChatRoomMember chatRoomMember = chatRoomMemberOpt.get();

            if (chatRoomMember.getLastReadMessageId() == null ||
                    messageId > chatRoomMember.getLastReadMessageId()) {
                chatRoomMember.updateLastReadMessageId(messageId);
            }
        }
    }


}
