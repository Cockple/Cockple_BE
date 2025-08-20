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
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.Request.ImageInfo;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.ChatProcessor;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;

import java.util.HashMap;
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
    private final MessageReadStatusRepository messageReadStatusRepository;

    private final SubscriptionService subscriptionService;
    private final MessageReadCreationService messageReadCreationService;
    private final ChatProcessor chatProcessor;
    private final ChatConverter chatConverter;
    private final ChatReadService chatReadService;

    private final ApplicationEventPublisher eventPublisher;

    public void sendMessage(Long chatRoomId, String content, List<FileInfo> files, List<ImageInfo> images, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        ChatRoom chatRoom = findChatRoom(chatRoomId);
        Member sender = findMemberWithProfile(senderId);

        String profileImageUrl = chatProcessor.generateProfileImageUrl(sender.getProfileImg());

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
        attachFiles(chatMessage, files);
        attachImages(chatMessage, images);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

        checkFirstMessageInDirect(chatRoomId, senderId, chatRoom);
        messageReadCreationService.createReadStatusForNewMessage(savedMessage, senderId);

        List<Long> activeSubscribers = subscriptionService.getActiveSubscribers(chatRoomId);
        int unreadCount = chatReadService.subscribersToReadStatus(chatRoom.getId(), savedMessage.getId(), activeSubscribers, senderId);

        List<ChatCommonDTO.ImageInfo> responseImages =
                createResponseImageInfos(savedMessage.getChatMessageImgs());
        List<WebSocketMessageDTO.MessageResponse.FileInfo> responseFiles =
                createResponseFileInfos(savedMessage.getChatMessageFiles());

        log.info("메시지 브로드캐스트 시작 - 채팅방 ID: {}", chatRoomId);
        WebSocketMessageDTO.MessageResponse response =
                chatConverter.toSendMessageResponse(chatRoomId, content, responseImages, responseFiles, savedMessage, sender, profileImageUrl, unreadCount);
        subscriptionService.broadcastMessage(chatRoomId, response, senderId);
        log.info("메시지 브로드캐스트 완료 - 채팅방 ID: {}", chatRoomId);

        publishChatRoomListUpdateEvent(chatRoom, savedMessage);
    }

    public void sendSystemMessage(Long partyId, String content) {
        ChatRoom chatRoom = findChatRoomByPartyId(partyId);

        ChatMessage systemMessage = ChatMessage.create(chatRoom, null, content, MessageType.SYSTEM);
        chatMessageRepository.save(systemMessage);

        WebSocketMessageDTO.MessageResponse broadcastSystemMessage
                = chatConverter.toSystemMessageResponse(chatRoom.getId(), content, systemMessage);

        subscriptionService.broadcastSystemMessage(chatRoom.getId(), broadcastSystemMessage);
        log.info("시스템 메시지 브로드캐스트 완료 - chatRoomId: {}", chatRoom.getId());
    }

    // ========== 비즈니스 메서드 ==========
    private void attachFiles(ChatMessage message, List<FileInfo> files) {
        if (files != null && !files.isEmpty()) {
            files.forEach(fileInfo -> {
                ChatMessageFile messageFile = ChatMessageFile.create(
                        message, fileInfo.originalFileName(),
                        fileInfo.fileKey(), fileInfo.fileSize(), fileInfo.fileType()
                );
                message.getChatMessageFiles().add(messageFile);
            });
        }
    }

    private void attachImages(ChatMessage message, List<ImageInfo> images) {
        if (images != null && !images.isEmpty()) {
            images.forEach(imageInfo -> {
                ChatMessageImg messageImg = ChatMessageImg.create(
                        message, imageInfo.imgKey(), imageInfo.imgOrder(),
                        imageInfo.originalFileName(), imageInfo.fileSize(), imageInfo.fileType()
                );
                message.getChatMessageImgs().add(messageImg);
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

    private List<ChatCommonDTO.ImageInfo> createResponseImageInfos(
            List<ChatMessageImg> savedImages) {
        return savedImages.stream()
                .map(img -> ChatCommonDTO.ImageInfo.builder()
                        .imageId(img.getId())
                        .imageUrl(chatProcessor.generateImageUrl(img))
                        .imgOrder(img.getImgOrder())
                        .isEmoji(img.getIsEmoji())
                        .originalFileName(img.getOriginalFileName())
                        .fileSize(img.getFileSize())
                        .fileType(img.getFileType())
                        .build())
                .toList();
    }

    private List<WebSocketMessageDTO.MessageResponse.FileInfo> createResponseFileInfos(
            List<ChatMessageFile> savedFiles) {
        return savedFiles.stream()
                .map(file -> WebSocketMessageDTO.MessageResponse.FileInfo.builder()
                        .fileId(file.getId())
                        .originalFileName(file.getOriginalFileName())
                        .fileSize(file.getFileSize())
                        .fileType(file.getFileType())
                        .build())
                .toList();
    }

    private void publishChatRoomListUpdateEvent(ChatRoom chatRoom, ChatMessage savedMessage) {
        try {
            List<Long> chatRoomMemberIds = chatRoomMemberRepository.findMemberIdsByChatRoomId(chatRoom.getId());

            Map<Long, Integer> memberUnreadCounts = calculateUnreadCountForMembers(
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

    private Map<Long, Integer> calculateUnreadCountForMembers(
            Long chatRoomId, List<Long> memberIds) {

        Map<Long, Integer> unreadCounts = new HashMap<>();

        for (Long memberId : memberIds) {
            try {
                Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);

                int unreadCount;
                if (memberOpt.isPresent()) {
                    Long lastReadMessageId = memberOpt.get().getLastReadMessageId();

                    if (lastReadMessageId == null) {
                        unreadCount = messageReadStatusRepository.countAllUnreadMessages(chatRoomId, memberId);
                    } else {
                        unreadCount = messageReadStatusRepository.countUnreadMessagesAfter(chatRoomId, memberId, lastReadMessageId);
                    }
                } else {
                    unreadCount = 0;
                }

                unreadCounts.put(memberId, unreadCount);

            } catch (Exception e) {
                log.error("멤버 {} 안 읽은 메시지 수 계산 실패 - 채팅방: {}", memberId, chatRoomId, e);
                unreadCounts.put(memberId, 0);
            }
        }

        return unreadCounts;
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
