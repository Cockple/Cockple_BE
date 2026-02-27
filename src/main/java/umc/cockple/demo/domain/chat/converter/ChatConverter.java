package umc.cockple.demo.domain.chat.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.domain.*;
import umc.cockple.demo.domain.chat.dto.*;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatConverter {

    // ===모임 채팅방 목록===
    public PartyChatRoomDTO.Response toPartyChatRoomListResponse(List<PartyChatRoomDTO.ChatRoomInfo> chatRoomInfos, boolean hasNext) {
        return PartyChatRoomDTO.Response.builder()
                .content(chatRoomInfos)
                .hasNext(hasNext)
                .build();
    }

    public PartyChatRoomDTO.Response toEmptyPartyChatRoomInfos() {
        return PartyChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
    }

    public PartyChatRoomDTO.ChatRoomInfo toPartyChatRoomInfo(ChatRoom chatRoom,
                                                             int memberCount,
                                                             int unreadCount,
                                                             PartyChatRoomDTO.LastMessageInfo lastMessageInfo,
                                                             String imgUrl) {
        return PartyChatRoomDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .partyId(chatRoom.getParty().getId())
                .partyName(chatRoom.getParty().getPartyName())
                .partyImgUrl(imgUrl)
                .memberCount(memberCount)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageInfo)
                .build();
    }

    public PartyChatRoomDTO.LastMessageInfo toPartyLastMessageInfo(LastMessageCacheDTO message) {
        if (message == null) return null;

        return PartyChatRoomDTO.LastMessageInfo.builder()
                .content(message.content())
                .timestamp(message.timestamp())
                .messageType(message.messageType())
                .build();
    }

    // ===개인 채팅방 목록===
    public DirectChatRoomDTO.Response toDirectChatRoomListResponse(List<DirectChatRoomDTO.ChatRoomInfo> chatRoomInfos, boolean hasNext) {
        return DirectChatRoomDTO.Response.builder()
                .content(chatRoomInfos)
                .hasNext(hasNext)
                .build();
    }

    public DirectChatRoomDTO.Response toEmptyDirectChatRoomInfos() {
        return DirectChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
    }

    public DirectChatRoomDTO.ChatRoomInfo toDirectChatRoomInfo(ChatRoom chatRoom,
                                                               ChatRoomMember chatRoomMember,
                                                               boolean isWithdrawn,
                                                               int unreadCount,
                                                               DirectChatRoomDTO.LastMessageInfo lastMessageInfo,
                                                               String imgUrl) {
        return DirectChatRoomDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .displayName(chatRoomMember.getDisplayName())
                .profileImgUrl(imgUrl)
                .isWithdrawn(isWithdrawn)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageInfo)
                .build();
    }

    public DirectChatRoomDTO.LastMessageInfo toDirectLastMessageInfo(LastMessageCacheDTO message) {
        if (message == null) return null;

        return DirectChatRoomDTO.LastMessageInfo.builder()
                .content(message.content())
                .timestamp(message.timestamp())
                .messageType(message.messageType())
                .build();
    }

    // ===개인 채팅방 생성===
    public DirectChatRoomCreateDTO.Response toDirectChatRoomCreateDTO(ChatRoom chatRoom, List<ChatRoomMember> members, String displayName) {
        return DirectChatRoomCreateDTO.Response.builder()
                .chatRoomId(chatRoom.getId())
                .displayName(displayName)
                .createdAt(chatRoom.getCreatedAt())
                .members(toMemberInfo(members))
                .build();
    }

    public List<DirectChatRoomCreateDTO.MemberInfo> toMemberInfo(List<ChatRoomMember> members) {
        return members.stream()
                .map(m -> DirectChatRoomCreateDTO.MemberInfo.builder()
                        .memberId(m.getMember().getId())
                        .memberName(m.getMember().getMemberName())
                        .build()
                ).collect(Collectors.toList());
    }

    public WebSocketMessageDTO.MessageResponse toSendMessageResponse(
            Long chatRoomId, String content,
            List<ChatCommonDTO.ImageInfo> images,
            List<WebSocketMessageDTO.MessageResponse.FileInfo> files,
            ChatMessage savedMessage, Member sender, String senderProfileImageUrl, int unreadCount) {
        return WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .messageId(savedMessage.getId())
                .content(content)
                .images(images)
                .files(files)
                .senderId(sender.getId())
                .senderName(sender.getMemberName())
                .senderProfileImageUrl(senderProfileImageUrl)
                .timestamp(savedMessage.getCreatedAt())
                .unreadCount(unreadCount)
                .build();
    }

    public WebSocketMessageDTO.MessageResponse toSystemMessageResponse(
            Long chatRoomId, String content, ChatMessage savedMessage) {
        return WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .messageId(savedMessage.getId())
                .content(content)
                .senderId(null)
                .senderName("시스템")
                .senderProfileImageUrl(null)
                .timestamp(savedMessage.getCreatedAt())
                .build();
    }

    public ChatRoomDetailDTO.ChatRoomInfo toChatRoomDetailChatRoomInfo(
            ChatRoom chatRoom,
            String displayName,
            String profileImageUrl,
            boolean isCounterPartWithdrawn,
            int memberCount,
            Long lastReadMessageId) {
        return ChatRoomDetailDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .chatRoomType(chatRoom.getType())
                .displayName(displayName)
                .profileImageUrl(profileImageUrl)
                .isCounterPartWithdrawn(isCounterPartWithdrawn)
                .memberCount(memberCount)
                .lastReadMessageId(lastReadMessageId)
                .build();
    }

    public ChatCommonDTO.MessageInfo toCommonMessageInfo(
            ChatMessage message,
            String senderProfileImageUrl,
            List<ChatCommonDTO.ImageInfo> processedImages,
            boolean isMyMessage) {

        return ChatCommonDTO.MessageInfo.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getMemberName())
                .senderProfileImageUrl(senderProfileImageUrl)
                .content(message.getContent())
                .messageType(message.getType())
                .images(processedImages)
                .timestamp(message.getCreatedAt())
                .isMyMessage(isMyMessage)
                .build();
    }

    public ChatCommonDTO.ImageInfo toImageInfo(ChatMessageImg img, String imageUrl) {
        return ChatCommonDTO.ImageInfo.builder()
                .imageId(img.getId())
                .imageUrl(imageUrl)
                .imgOrder(img.getImgOrder())
                .isEmoji(img.getIsEmoji())
                .originalFileName(img.getOriginalFileName())
                .fileSize(img.getFileSize())
                .fileType(img.getFileType())
                .build();
    }

    public List<ChatRoomDetailDTO.MessageInfo> toChatRoomDetailMessageInfos(
            List<ChatCommonDTO.MessageInfo> commonMessages) {
        return commonMessages.stream()
                .map(ChatRoomDetailDTO.MessageInfo::from)
                .toList();
    }

    public ChatRoomDetailDTO.MemberInfo toChatRoomDetailMemberInfo(Member member, String memberProfileImgUrl) {
        return ChatRoomDetailDTO.MemberInfo.builder()
                .memberId(member.getId())
                .memberName(member.getMemberName())
                .profileImgUrl(memberProfileImgUrl)
                .build();
    }

    public ChatRoomDetailDTO.Response toChatRoomDetailResponse(
            ChatRoomDetailDTO.ChatRoomInfo roomInfo,
            List<ChatRoomDetailDTO.MessageInfo> messageInfos,
            List<ChatRoomDetailDTO.MemberInfo> memberInfos) {
        return ChatRoomDetailDTO.Response.builder()
                .chatRoomInfo(roomInfo)
                .messages(messageInfos)
                .participants(memberInfos)
                .build();
    }

    public List<ChatMessageDTO.MessageInfo> toChatMessageInfos(
            List<ChatCommonDTO.MessageInfo> commonMessages) {
        return commonMessages.stream()
                .map(ChatMessageDTO.MessageInfo::from)
                .toList();
    }

    public ChatMessageDTO.Response toChatMessageResponse(
            List<ChatMessageDTO.MessageInfo> messages, boolean hasNext, Long nextCursor) {
        return ChatMessageDTO.Response.builder()
                .messages(messages)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    public ChatDownloadTokenDTO.Response toDownloadTokenResponse(DownloadToken token, int validityInSeconds) {
        String downloadUrl = String.format("/api/chats/files/%d/download?token=%s", token.getFileId(), token.getToken());
        return ChatDownloadTokenDTO.Response.builder()
                .downloadToken(token.getToken())
                .downloadUrl(downloadUrl)
                .expiresIn(validityInSeconds)
                .expiresAt(token.getExpiresAt())
                .build();
    }

    public PartyChatRoomIdDTO toChatRoomIdDTO(ChatRoom chatRoom) {
        return PartyChatRoomIdDTO.builder()
                .roomId(chatRoom.getId())
                .build();
    }
}
