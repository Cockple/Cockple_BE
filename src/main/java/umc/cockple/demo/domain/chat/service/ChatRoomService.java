package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.websocket.ChatListSubscriptionService;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.RedisSubscriptionService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatFileRepository chatFileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomListCacheService chatRoomListCacheService;
    private final RedisSubscriptionService redisSubscriptionService;
    private final ChatListSubscriptionService chatListSubscriptionService;

    public void createPartyChatRoom(Party party, Member owner) {
        log.info("[모임 채팅방 생성 시작] - partyId: {}", party.getId());
        ChatRoom chatRoom = ChatRoom.createPartyChatRoom(party);
        ChatRoomMember chatRoomMember = ChatRoomMember.create(chatRoom, owner);

        chatRoom.addChatRoomMember(chatRoomMember);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("[모임 채팅방 생성 완료] - chatRoomId: {}", savedChatRoom.getId());
    }

    public void joinPartyChatRoom(Long partyId, Member member) {
        log.info("[모임 채팅방 자동 참여 시작] - memberId: {}, partyId: {}", member.getId(), partyId);
        ChatRoom chatRoom = findChatRoomByPartyIdOrThrow(partyId);
        ChatRoomMember chatRoomMember = ChatRoomMember.create(chatRoom, member);

        chatRoomMemberRepository.save(chatRoomMember);
        log.info("[모임 채팅방 자동 참여 완료]  - chatRoomId: {}", chatRoom.getId());
    }

    public void leavePartyChatRoom(Long partyId, Long memberId) {
        log.info("[모임 채팅방 퇴장 시작] - memberId: {}, partyId:{}", memberId, partyId);
        ChatRoom chatRoom = findChatRoomByPartyIdOrThrow(partyId);

        chatRoomMemberRepository
                .findByChatRoomIdAndMemberId(chatRoom.getId(), memberId)
                .ifPresent(chatRoomMemberRepository::delete);
        log.info("[모임 채팅방 퇴장 완료] - chatRoomId: {}", chatRoom.getId());
    }

    public void deletePartyChatRoom(Long partyId) {
        log.info("[모임 채팅방 삭제 시작] - partyId: {}", partyId);

        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findByPartyId(partyId);

        if (chatRoomOptional.isEmpty()) {
            log.warn("[모임 채팅방 삭제 스킵] - partyId: {}, 채팅방이 존재하지 않습니다.", partyId);
            return;
        }

        ChatRoom chatRoom = chatRoomOptional.get();
        Long chatRoomId = chatRoom.getId();

        messageReadStatusRepository.deleteByChatRoomId(chatRoomId);
        chatRoomListCacheService.evictLastMessage(chatRoomId);
        redisSubscriptionService.clearRoomSubscribers(chatRoomId);
        chatListSubscriptionService.clearChatListSubscribers(chatRoomId);
        chatFileRepository.deleteByChatRoomId(chatRoomId);
        chatMessageRepository.deleteByChatRoomId(chatRoomId);
        chatRoomMemberRepository.deleteByChatRoomId(chatRoomId);
        chatRoomRepository.deleteRoomById(chatRoomId);

        log.info("[모임 채팅방 삭제 완료] - partyId: {}, chatRoomId: {}", partyId, chatRoomId);
    }

    private ChatRoom findChatRoomByPartyIdOrThrow(Long partyId) {
        return chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }
}
