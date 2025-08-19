package umc.cockple.demo.domain.chat.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.*;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.Request.FileInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.Request.ImageInfo;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
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

import java.util.List;
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

    private final ImageService imageService;
    private final SubscriptionService subscriptionService;
    private final MessageReadCreationService messageReadCreationService;

    private final ChatConverter chatConverter;
    private final ChatReadService chatReadService;

    public void sendMessage(Long chatRoomId, String content, List<FileInfo> files, List<ImageInfo> images, Long senderId) {
        log.info("메시지 전송 시작 - 채팅방: {}, 발신자: {}", chatRoomId, senderId);

        ChatRoom chatRoom = findChatRoom(chatRoomId);
        Member sender = findMemberWithProfile(senderId);

        String profileImageUrl = getImageUrl(sender.getProfileImg());

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
        attachFiles(chatMessage, files);
        attachImages(chatMessage, images);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

        checkFirstMessageInDirect(chatRoomId, senderId, chatRoom);
        messageReadCreationService.createReadStatusForNewMessage(savedMessage, senderId);

        List<Long> activeSubscribers = subscriptionService.getActiveSubscribers(chatRoomId);
        int unreadCount = chatReadService.subscribersToReadStatus(chatRoom.getId(), savedMessage.getId(), activeSubscribers, senderId);

        List<WebSocketMessageDTO.MessageResponse.ImageInfo> responseImages =
                createResponseImageInfos(savedMessage.getChatMessageImgs());
        List<WebSocketMessageDTO.MessageResponse.FileInfo> responseFiles =
                createResponseFileInfos(savedMessage.getChatMessageFiles());

        log.info("메시지 브로드캐스트 시작 - 채팅방 ID: {}", chatRoomId);
        WebSocketMessageDTO.MessageResponse response =
                chatConverter.toSendMessageResponse(chatRoomId, content, responseImages, responseFiles, savedMessage, sender, profileImageUrl, unreadCount);
        subscriptionService.broadcastMessage(chatRoomId, response, senderId);
        log.info("메시지 브로드캐스트 완료 - 채팅방 ID: {}", chatRoomId);
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

    private List<WebSocketMessageDTO.MessageResponse.ImageInfo> createResponseImageInfos(
            List<ChatMessageImg> savedImages) {
        return savedImages.stream()
                .map(img -> WebSocketMessageDTO.MessageResponse.ImageInfo.builder()
                        .imageId(img.getId())
                        .imageUrl(imageService.getUrlFromKey(img.getImgKey()))
                        .imgOrder(img.getImgOrder())
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

    private String getImageUrl(ProfileImg profileImg) {
        if (profileImg != null && profileImg.getImgKey() != null && !profileImg.getImgKey().isBlank()) {
            return imageService.getUrlFromKey(profileImg.getImgKey());
        }
        return null;
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
