package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;

    public WebSocketMessageDTO.Response sendMessage(Long chatRoomId, String content, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        ChatRoom chatRoom = findChatRoomOrThrow(chatRoomId);
        Member sender = findMemberOrThrow(senderId);

        validateChatRoomMember(chatRoom, sender);

        // TODO: 다양한 타입의 텍스트 전송가능하도록 변경해야 함
        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

    }

    private void validateChatRoomMember(ChatRoom chatRoom, Member member) {
        if (!chatRoomRepository.existsByChatRoomAndMember(chatRoom, member))
            throw new ChatException(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
    }

    private ChatRoom findChatRoomOrThrow(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long senderId) {
        return memberRepository.findById(senderId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MEMBER_NOT_FOUND));
    }
}
