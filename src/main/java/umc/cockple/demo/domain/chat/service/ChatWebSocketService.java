package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.MemberConnectionInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.repository.MemberRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final ImageService imageService;
    private final SubscriptionService subscriptionService;

    private final ChatConverter chatConverter;

    public MemberConnectionInfo getMemberConnectionInfo(Long memberId) {
        log.debug("멤버 연결 정보 조회 - memberId: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MEMBER_NOT_FOUND));

        return MemberConnectionInfo.builder()
                .memberId(memberId)
                .memberName(member.getMemberName())
                .build();
    }

    public void sendMessage(Long chatRoomId, String content, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        validateSendMessage(chatRoomId, content, senderId);
        ChatRoom chatRoom = findChatRoomOrThrow(chatRoomId);
        Member sender = findMemberWithProfileOrThrow(senderId);

        String profileImageUrl = getImageUrl(sender.getProfileImg());

        // TODO: 다양한 타입의 텍스트 전송가능하도록 변경해야 함
        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

        log.info("메시지 브로드캐스트 시작 - 채팅방 ID: {}", chatRoomId);
        WebSocketMessageDTO.MessageResponse response =
                chatConverter.toSendMessageResponse(chatRoomId, content, savedMessage, sender, profileImageUrl);
        subscriptionService.broadcastMessage(chatRoomId, response, senderId);
        log.info("메시지 브로드캐스트 완료 - 채팅방 ID: {}", chatRoomId);
    }

    public void sendSystemMessage(Long partyId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId);

        ChatMessage systemMessage = ChatMessage.create(chatRoom, null, content, MessageType.SYSTEM);
        chatMessageRepository.save(systemMessage);

        WebSocketMessageDTO.MessageResponse broadcastSystemMessage
                = chatConverter.toSystemMessageResponse(chatRoom.getId(), content, systemMessage);

        subscriptionService.broadcastSystemMessage(chatRoom.getId(), broadcastSystemMessage);
        log.info("시스템 메시지 브로드캐스트 완료 - chatRoomId: {}", chatRoom.getId());
    }

    public void readMessage(Long chatRoomId, Long lastReadMessageId, Long readerId) {
        validateChatRoomMember(chatRoomId, readerId);
        updateLastReadMessage(chatRoomId, lastReadMessageId, readerId);

        WebSocketMessageDTO.ReadResponse broadcastReadMessage
                = chatConverter.toReadResponse(chatRoomId, lastReadMessageId, readerId);

        subscriptionService.broadcastReadNotification(chatRoomId, broadcastReadMessage, readerId);
        log.info("읽음 브로드캐스트 완료 - chatRoomId: {}, lastReadMessageId: {}", chatRoomId, lastReadMessageId);
    }

    // ========= 비즈니스 메서드 ==========

    private void updateLastReadMessage(Long chatRoomId, Long lastReadMessageId, Long readerId) {
        chatRoomMemberRepository.updateLastReadMessageIfNewer(chatRoomId, lastReadMessageId, readerId);
    }

    // ========== 검증 메서드 ==========

    private void validateSendMessage(Long chatRoomId, String content, Long senderId) {
        validateChatRoom(chatRoomId);
        validateContent(content);
        validateChatRoomMember(chatRoomId, senderId);
    }

    // ========== 세부 검증 메서드 ==========

    private void validateChatRoom(Long chatRoomId) {
        if (chatRoomId == null) {
            throw new ChatException(ChatErrorCode.CHATROOM_ID_NECESSARY);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ChatException(ChatErrorCode.CONTENT_NECESSARY);
        }

        if (content.length() > 1000) {
            throw new ChatException(ChatErrorCode.MESSAGE_TO_LONG);
        }
    }

    private void validateChatRoomMember(Long chatRoomId, Long memberId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId))
            throw new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
    }

    private String getImageUrl(ProfileImg profileImg) {
        if (profileImg != null && profileImg.getImgKey() != null && !profileImg.getImgKey().isBlank()) {
            return imageService.getUrlFromKey(profileImg.getImgKey());
        }
        return null;
    }

    private ChatRoom findChatRoomOrThrow(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private Member findMemberWithProfileOrThrow(Long senderId) {
        return memberRepository.findMemberWithProfileById(senderId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MEMBER_NOT_FOUND));
    }
}
