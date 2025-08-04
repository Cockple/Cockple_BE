package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class DirectChatRoomDTO {

    // 채팅방 목록 응답
    @Builder
    public record Response(
            List<ChatRoomInfo> content,
            int totalElements
    ) {
    }

    // 채팅방 하나에 대한 정보
    @Builder
    public record ChatRoomInfo(
            Long chatRoomId,
            String displayName,
            String profileImgUrl,
            int unreadCount,
            LastMessageInfo lastMessage
    ) {
    }

    // 마지막 메시지 정보
    @Builder
    public record LastMessageInfo(
            Long messageId,
            String content,
            LocalDateTime timestamp,
            String messageType
    ) {
    }
}
