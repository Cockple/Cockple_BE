package umc.cockple.demo.domain.chat.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.List;

@Component
public class ChatWebSocketResponseAssembler {

    private static final String SYSTEM_USER_NAME = "시스템";

    public WebSocketMessageDTO.MessageResponse toSendMessageResponse(
            Long chatRoomId, String content,
            List<ChatCommonDTO.FileInfo> files,
            ChatMessage savedMessage, Member sender, String senderProfileImageUrl, int unreadCount) {
        return WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .messageId(savedMessage.getId())
                .content(content)
                .messageType(savedMessage.getType())
                .images(files)
                .senderId(sender.getId())
                .senderName(sender.getDisplayName())
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
                .messageType(savedMessage.getType())
                .senderId(null)
                .senderName(SYSTEM_USER_NAME)
                .senderProfileImageUrl(null)
                .timestamp(savedMessage.getCreatedAt())
                .build();
    }
}
