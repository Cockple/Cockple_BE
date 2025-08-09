package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final WebSocketBroadcastService broadcastService;

    public void joinPartyChatRoom(Long partyId, Long memberId) {
        log.info("모임 채팅방 자동 참여 시작 - memberId: {}, partyId: {}", memberId, partyId);
        Member member = findMemberOrThrow(memberId);
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId);

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();

        chatRoomMemberRepository.save(chatRoomMember);
        log.info("모임 채팅방 자동 참여 완료  - chatRoomId: {}", chatRoom.getId());
    }

    public void leavePartyChatRoom(Long partyId, Long memberId) {
        log.info("모임 채팅방 퇴장 시작 - memberId: {}, partyId:{}", memberId, partyId);
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId);

        chatRoomMemberRepository
                .findByChatRoomIdAndMemberId(chatRoom.getId(), memberId)
                .ifPresent(chatRoomMemberRepository::delete);
        log.info("모임 채팅방 퇴장 완료 - chatRoomId: {}", chatRoom.getId());
    }

    public void sendSystemMessage(Long partyId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId);

        ChatMessage systemMessage = ChatMessage.create(chatRoom, null, content, MessageType.SYSTEM);
        chatMessageRepository.save(systemMessage);

        WebSocketMessageDTO.Response broadcastSystemMessage = WebSocketMessageDTO.Response.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoom.getId())
                .senderId(null)
                .senderName("시스템")
                .senderProfileImageUrl(null)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        broadcastService.broadcastToChatRoom(chatRoom.getId(), broadcastSystemMessage);
        log.info("시스템 메시지 브로드캐스트 완료 - chatRoomId: {}", chatRoom.getId());
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MEMBER_NOT_FOUND));
    }
}
