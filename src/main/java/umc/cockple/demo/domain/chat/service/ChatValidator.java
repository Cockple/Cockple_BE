package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.Request.FileInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.Request.ImageInfo;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatValidator {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void validateSendRequest(Long chatRoomId, String content, List<FileInfo> files, List<ImageInfo> images, Long senderId) {
        validateChatRoom(chatRoomId);
        validateChatRoomMember(chatRoomId, senderId);
        validateMessage(content, files, images);
    }

    public void validateSubscriptionRequest(Long chatRoomId, Long senderId) {
        validateChatRoom(chatRoomId);
        validateChatRoomMember(chatRoomId, senderId);
    }

    public void validateUnsubscriptionRequest(Long chatRoomId, Long senderId) {
        validateChatRoom(chatRoomId);
        validateChatRoomMember(chatRoomId, senderId);
    }

    public void validateChatListSubscriptionRequest(Long memberId, List<Long> chatRoomIds) {
        validateChatRoomIds(chatRoomIds);
        validateChatRoomId(memberId, chatRoomIds);
    }

    public void validateChatListUnsubscriptionRequest(Long memberId, List<Long> chatRoomIds) {
        validateChatRoomIds(chatRoomIds);
        validateChatRoomId(memberId, chatRoomIds);
    }

    // ========== 세부 검증 메서드 ==========

    private void validateChatRoom(Long chatRoomId) {
        if (chatRoomId == null) {
            throw new ChatException(ChatErrorCode.CHATROOM_ID_NECESSARY);
        }

        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    private void validateChatRoomMember(Long chatRoomId, Long memberId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId))
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    private void validateMessage(String content, List<FileInfo> files, List<ImageInfo> images) {
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasFiles = files != null && !files.isEmpty();
        boolean hasImages = images != null && !images.isEmpty();

        if (!hasContent && !hasFiles && !hasImages) {
            throw new ChatException(ChatErrorCode.EMPTY_MESSAGE_NOT_ALLOWED);
        }

        if (content != null && content.length() > 1000) {
            throw new ChatException(ChatErrorCode.MESSAGE_TO_LONG);
        }
    }

    private static void validateChatRoomIds(List<Long> chatRoomIds) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            throw new ChatException(ChatErrorCode.CHATROOM_LIST_EMPTY);
        }

        if (chatRoomIds.size() > 100) {
            throw new ChatException(ChatErrorCode.TOO_MANY_CHATROOMS);
        }
    }

    private void validateChatRoomId(Long memberId, List<Long> chatRoomIds) {
        List<Long> uniqueRoomIds = chatRoomIds.stream().distinct().toList();

        for (Long chatRoomId : uniqueRoomIds) {
            if (chatRoomId == null || chatRoomId <= 0) {
                throw new ChatException(ChatErrorCode.INVALID_CHATROOM_ID);
            }

            if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
                throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }
        }
    }
}
