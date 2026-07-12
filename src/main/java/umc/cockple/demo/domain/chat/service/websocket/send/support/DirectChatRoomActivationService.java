package umc.cockple.demo.domain.chat.service.websocket.send.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DirectChatRoomActivationService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void joinPendingMemberOnFirstMessage(ChatRoom chatRoom, Long senderId) {
        if (chatRoom.getType() != ChatRoomType.DIRECT || !isFirstMessage(chatRoom.getId())) {
            return;
        }

        log.info("첫 번째 개인 메시지 처리 - 채팅방: {}", chatRoom.getId());
        chatRoomMemberRepository.findPendingMemberInDirect(chatRoom.getId(), senderId)
                .ifPresent(this::joinPendingMember);
    }

    private boolean isFirstMessage(Long chatRoomId) {
        return chatMessageRepository.countByChatRoomId(chatRoomId) == 1;
    }

    private void joinPendingMember(ChatRoomMember pendingMember) {
        pendingMember.joinChatRoom();
        log.info("PENDING 멤버를 JOINED로 변경 완료 - 멤버 ID: {}", pendingMember.getMember().getId());
    }
}
