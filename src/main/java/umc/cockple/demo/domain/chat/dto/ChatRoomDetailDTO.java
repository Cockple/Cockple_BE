package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomDetailDTO {

    @Builder
    public record Response(
            ChatRoomInfo chatRoomInfo,
            List<MessageInfo> messages,
            List<MemberInfo> participants
    ){
    }

    @Builder
    public record ChatRoomInfo(
            Long chatRoomId,
            ChatRoomType chatRoomType,
            String displayName,
            String profileImageUrl,
            int memberCount,
            Long lastReadMessageId
    ) {
    }

    @Builder
    public record MessageInfo(
            Long messageId,
            Long senderId,
            String senderName,
            String senderProfileImageUrl,
            String content,
            MessageType messageType,
            List<ChatCommonDTO.ImageInfo> images,
            LocalDateTime timestamp,
            boolean isMyMessage
    ) {
        public static MessageInfo from(ChatCommonDTO.MessageInfo common) {
            return MessageInfo.builder()
                    .messageId(common.messageId())
                    .senderId(common.senderId())
                    .senderName(common.senderName())
                    .senderProfileImageUrl(common.senderProfileImageUrl())
                    .content(common.content())
                    .messageType(common.messageType())
                    .images(common.images())
                    .timestamp(common.timestamp())
                    .isMyMessage(common.isMyMessage())
                    .build();
        }
    }

    @Builder
    public record MemberInfo(
            Long memberId,
            String memberName,
            String profileImgUrl
    ) {
        public static MemberInfo from(ChatCommonDTO.MemberInfo common) {
            return MemberInfo.builder()
                    .memberId(common.memberId())
                    .memberName(common.memberName())
                    .profileImgUrl(common.profileImgUrl())
                    .build();
        }
    }
}
