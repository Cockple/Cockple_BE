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
            List<String> imageUrls,  // 이미지 메시지인 경우
            LocalDateTime timestamp,
            boolean isMyMessage    // 내가 보낸 메시지인지
    ) {
    }

    @Builder
    public record MemberInfo(
            Long memberId,
            String memberName,
            String profileImgUrl
    ) {
    }
}
