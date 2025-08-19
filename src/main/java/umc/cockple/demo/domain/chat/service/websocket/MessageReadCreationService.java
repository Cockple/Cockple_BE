package umc.cockple.demo.domain.chat.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageReadCreationService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;

    public void createReadStatusForNewMessage(ChatMessage chatMessage, Long senderId) {
        log.info("메시지 읽음 상태 생성 시작 - 메시지 ID: {}, 발신자: {}", chatMessage.getId(), senderId);
        Long chatRoomId = chatMessage.getChatRoom().getId();
        Long messageId = chatMessage.getId();

        List<Long> memberIds = chatRoomMemberRepository.findMemberIdsByChatRoomId(chatRoomId);

        List<MessageReadStatus> readStatuses = memberIds.stream()
                .map(memberId -> {
                    boolean isRead = memberId.equals(senderId);

                    if (isRead) {
                        return MessageReadStatus.createRead(messageId, memberId, chatRoomId);
                    } else {
                        return MessageReadStatus.createUnread(messageId, memberId, chatRoomId);
                    }
                })
                .toList();

        messageReadStatusRepository.saveAll(readStatuses);
    }
}
