package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
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

    public WebSocketMessageDTO.Response sendMessage(Long chatRoomId, String content, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        ChatRoom chatRoom = findChatRoomOrThrow(chatRoomId);
        Member sender = findMemberOrThrow(senderId);

        validateChatRoomMember(chatRoom, sender);
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
