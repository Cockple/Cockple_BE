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
    }


}
