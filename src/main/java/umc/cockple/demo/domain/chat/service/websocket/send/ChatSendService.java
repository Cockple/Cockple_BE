package umc.cockple.demo.domain.chat.service.websocket.send;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatWebSocketResponseAssembler;
import umc.cockple.demo.domain.chat.domain.*;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.service.ChatProcessor;
import umc.cockple.demo.domain.chat.service.websocket.send.support.ChatMessageFileAppender;
import umc.cockple.demo.domain.chat.service.websocket.send.support.ChatSendEventPublisher;
import umc.cockple.demo.domain.chat.service.websocket.send.support.DirectChatRoomActivationService;
import umc.cockple.demo.domain.chat.service.support.reader.ChatMemberReader;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomReader;
import umc.cockple.demo.domain.chat.service.websocket.ChatReadService;
import umc.cockple.demo.domain.chat.service.websocket.MessageReadCreationService;
import umc.cockple.demo.domain.chat.service.websocket.SubscriptionService;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatSendService {

    private final ChatMessageRepository chatMessageRepository;

    private final ChatRoomReader chatRoomReader;
    private final ChatMemberReader chatMemberReader;
    private final ChatMessageFileAppender chatMessageFileAppender;
    private final DirectChatRoomActivationService directChatRoomActivationService;
    private final ChatSendEventPublisher chatSendEventPublisher;
    private final SubscriptionService subscriptionService;
    private final MessageReadCreationService messageReadCreationService;
    private final ChatProcessor chatProcessor;
    private final ChatWebSocketResponseAssembler chatWebSocketResponseAssembler;
    private final ChatReadService chatReadService;

    public void sendMessage(Long chatRoomId, String content, List<WebSocketMessageDTO.Request.FileInfo> files, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        ChatRoom chatRoom = chatRoomReader.read(chatRoomId);
        Member sender = chatMemberReader.readWithProfile(senderId);

        String profileImageUrl = chatProcessor.generateProfileImageUrl(sender.getProfileImg());

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
        chatMessageFileAppender.append(chatMessage, files);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

        directChatRoomActivationService.joinPendingMemberOnFirstMessage(chatRoom, senderId);
        messageReadCreationService.createReadStatusForNewMessage(savedMessage, senderId);

        List<Long> activeSubscribers = subscriptionService.getActiveSubscribers(chatRoomId);
        int unreadCount = chatReadService.subscribersToReadStatus(chatRoom.getId(), savedMessage.getId(), activeSubscribers, senderId);

        List<ChatCommonDTO.FileInfo> responseFiles =
                createResponseFileInfos(savedMessage.getChatMessageFiles());

        log.info("메시지 브로드캐스트 시작 - 채팅방 ID: {}", chatRoomId);
        WebSocketMessageDTO.MessageResponse response =
                chatWebSocketResponseAssembler.toSendMessageResponse(chatRoomId, content, responseFiles, savedMessage, sender, profileImageUrl, unreadCount);
        subscriptionService.broadcastMessage(chatRoomId, response, senderId);
        log.info("메시지 브로드캐스트 완료 - 채팅방 ID: {}", chatRoomId);

        chatSendEventPublisher.publishChatNotificationEvent(chatRoom, savedMessage, sender, activeSubscribers);
        chatSendEventPublisher.publishChatRoomListUpdateEvent(chatRoom, savedMessage);
        chatSendEventPublisher.publishUnreadStatusUpdateEvent(chatRoom, senderId);
    }

    public void sendSystemMessage(Long partyId, String content) {
        ChatRoom chatRoom = chatRoomReader.readByPartyId(partyId);

        ChatMessage systemMessage = ChatMessage.create(chatRoom, null, content, MessageType.SYSTEM);
        ChatMessage savedSystemMessage = chatMessageRepository.save(systemMessage);

        messageReadCreationService.createReadStatusForNewMessage(savedSystemMessage, null);

        List<Long> activeSubscribers = subscriptionService.getActiveSubscribers(chatRoom.getId());
        chatReadService.subscribersToReadStatus(
                chatRoom.getId(),
                savedSystemMessage.getId(),
                activeSubscribers,
                null
        );

        WebSocketMessageDTO.MessageResponse broadcastSystemMessage
                = chatWebSocketResponseAssembler.toSystemMessageResponse(chatRoom.getId(), content, savedSystemMessage);

        subscriptionService.broadcastSystemMessage(chatRoom.getId(), broadcastSystemMessage);
        chatSendEventPublisher.publishChatRoomListUpdateEvent(chatRoom, savedSystemMessage);
        log.info("시스템 메시지 브로드캐스트 완료 - chatRoomId: {}", chatRoom.getId());
    }

    // ========== 비즈니스 메서드 ==========
    private List<ChatCommonDTO.FileInfo> createResponseFileInfos(
            List<ChatMessageFile> savedFiles) {
        return savedFiles.stream()
                .map(file -> ChatCommonDTO.FileInfo.builder()
                        .imageId(file.getId())
                        .imageUrl(chatProcessor.generateFileUrl(file))
                        .imgOrder(file.getFileOrder())
                        .isEmoji(file.getIsEmoji())
                        .originalFileName(file.getOriginalFileName())
                        .fileSize(file.getFileSize())
                        .fileType(file.getFileType())
                        .build())
                .toList();
    }

}
