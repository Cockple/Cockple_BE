package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import lombok.Builder;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate;

@Builder
public record ChatRoomListUpdateData(
        LastMessageUpdate lastMessage,
        int unreadCount
) {
}
