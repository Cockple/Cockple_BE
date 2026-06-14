package umc.cockple.demo.domain.chat.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.*;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.Request.FileInfo;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.service.ChatProcessor;
import umc.cockple.demo.domain.chat.service.ChatUnreadQueryService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.events.ChatNotificationEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatSendService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final SubscriptionService subscriptionService;
    private final MessageReadCreationService messageReadCreationService;
    private final ChatProcessor chatProcessor;
    private final ChatConverter chatConverter;
    private final ChatReadService chatReadService;
    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public void sendMessage(Long chatRoomId, String content, List<WebSocketMessageDTO.Request.FileInfo> files, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        ChatRoom chatRoom = findChatRoom(chatRoomId);
        Member sender = findMemberWithProfile(senderId);

        String profileImageUrl = chatProcessor.generateProfileImageUrl(sender.getProfileImg());

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
        attachFiles(chatMessage, files);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

        checkFirstMessageInDirect(chatRoomId, senderId, chatRoom);
        messageReadCreationService.createReadStatusForNewMessage(savedMessage, senderId);

        List<Long> activeSubscribers = subscriptionService.getActiveSubscribers(chatRoomId);
        int unreadCount = chatReadService.subscribersToReadStatus(chatRoom.getId(), savedMessage.getId(), activeSubscribers, senderId);

        List<ChatCommonDTO.FileInfo> responseFiles =
                createResponseFileInfos(savedMessage.getChatMessageFiles());

        log.info("메시지 브로드캐스트 시작 - 채팅방 ID: {}", chatRoomId);
        WebSocketMessageDTO.MessageResponse response =
                chatConverter.toSendMessageResponse(chatRoomId, content, responseFiles, savedMessage, sender, profileImageUrl, unreadCount);
        subscriptionService.broadcastMessage(chatRoomId, response, senderId);
        log.info("메시지 브로드캐스트 완료 - 채팅방 ID: {}", chatRoomId);

        // 알림 이벤트 발행
        publishChatNotificationEvent(chatRoom, savedMessage, sender, activeSubscribers);

        publishChatRoomListUpdateEvent(chatRoom, savedMessage);
    }

    public void sendSystemMessage(Long partyId, String content) {
        ChatRoom chatRoom = findChatRoomByPartyId(partyId);

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
                = chatConverter.toSystemMessageResponse(chatRoom.getId(), content, savedSystemMessage);

        subscriptionService.broadcastSystemMessage(chatRoom.getId(), broadcastSystemMessage);
        publishChatRoomListUpdateEvent(chatRoom, savedSystemMessage);
        log.info("시스템 메시지 브로드캐스트 완료 - chatRoomId: {}", chatRoom.getId());
    }

    // ========== 비즈니스 메서드 ==========
    private void attachFiles(ChatMessage message, List<WebSocketMessageDTO.Request.FileInfo> files) {
        if (files != null && !files.isEmpty()) {
            files.forEach(fileInfo -> {
                ChatMessageFile messageFile = ChatMessageFile.create(
                        message, fileInfo.imgKey(), fileInfo.imgOrder(),
                        fileInfo.originalFileName(), fileInfo.fileSize(), fileInfo.fileType()
                );
                message.getChatMessageFiles().add(messageFile);
            });
        }
    }

    private void checkFirstMessageInDirect(Long chatRoomId, Long senderId, ChatRoom chatRoom) {
        if (chatRoom.getType() == ChatRoomType.DIRECT && isFirstMessage(chatRoomId)) {
            handleFirstDirectMessage(chatRoomId, senderId);
        }
    }

    private boolean isFirstMessage(Long chatRoomId) {
        return chatMessageRepository.countByChatRoomId(chatRoomId) == 1;
    }

    private void handleFirstDirectMessage(Long chatRoomId, Long senderId) {
        log.info("첫 번째 개인 메시지 처리 - 채팅방: {}", chatRoomId);
        Optional<ChatRoomMember> pendingMemberOpt = chatRoomMemberRepository.findPendingMemberInDirect(chatRoomId, senderId);

        if (pendingMemberOpt.isPresent()) {
            ChatRoomMember pendingMember = pendingMemberOpt.get();
            pendingMember.joinChatRoom();

            Long targetMemberId = pendingMember.getMember().getId();
            log.info("PENDING 멤버를 JOINED로 변경 완료 - 멤버 ID: {}", targetMemberId);
        }
    }

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

    private void publishChatRoomListUpdateEvent(ChatRoom chatRoom, ChatMessage savedMessage) {
        try {
            List<Long> chatRoomMemberIds = chatRoomMemberRepository.findMemberIdsByChatRoomId(chatRoom.getId());

            Map<Long, Integer> memberUnreadCounts = chatUnreadQueryService.countUnreadMessagesByMembers(
                    chatRoom.getId(), chatRoomMemberIds);

            ChatRoomListUpdateEvent listUpdateEvent = ChatRoomListUpdateEvent.create(
                    chatRoom.getId(),
                    savedMessage.getDisplayContent(),
                    savedMessage.getCreatedAt(),
                    savedMessage.getType().name(),
                    memberUnreadCounts
            );

            eventPublisher.publishEvent(listUpdateEvent);
            log.info("채팅방 목록 업데이트 이벤트 발행 - 채팅방: {}", chatRoom.getId());

        } catch (Exception e) {
            log.error("채팅방 목록 업데이트 이벤트 발행 실패 - 채팅방: {}", chatRoom.getId(), e);
        }
    }

    // 채팅 알림 이벤트 발행
    private void publishChatNotificationEvent(ChatRoom chatRoom, ChatMessage savedMessage,
                                              Member sender, List<Long> activeSubscribers) {
        String notificationTitle = chatRoom.getType() == ChatRoomType.PARTY
                ? chatRoom.getParty().getPartyName()
                : sender.getNickname();
        String notificationContent = chatRoom.getType() == ChatRoomType.PARTY
                ? sender.getNickname() + ": " + savedMessage.getDisplayContent()
                : savedMessage.getDisplayContent();

        eventPublisher.publishEvent(ChatNotificationEvent.create(
                chatRoom.getId(),
                chatRoom.getType(),
                notificationTitle,
                notificationContent,
                sender.getId(),
                activeSubscribers
        ));
    }

    private ChatRoom findChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom findChatRoomByPartyId(Long partyId) {
        return chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private Member findMemberWithProfile(Long senderId) {
        return memberRepository.findMemberWithProfileById(senderId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MEMBER_NOT_FOUND));
    }
}
