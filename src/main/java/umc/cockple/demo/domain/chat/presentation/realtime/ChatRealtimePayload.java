package umc.cockple.demo.domain.chat.presentation.realtime;

import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;

import java.util.List;

public record ChatRealtimePayload(
        Long chatRoomId,
        List<Long> memberRooms,
        String content,
        List<WebSocketMessageDTO.Request.FileInfo> images,
        Long lastReadMessageId
) {

    public WebSocketMessageDTO.Request toCommandRequest(ChatRealtimeAction action) {
        return new WebSocketMessageDTO.Request(
                action.commandType(),
                chatRoomId,
                memberRooms,
                content,
                images,
                lastReadMessageId
        );
    }
}
