package umc.cockple.demo.domain.chat.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.image.service.ImageService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatConverter {

    private final ImageService imageService;

    public PartyChatRoomDTO.Response toPartyChatRoomListResponse(List<PartyChatRoomDTO.ChatRoomInfo> chatRoomInfos) {
        return PartyChatRoomDTO.Response.builder()
                .content(chatRoomInfos)
                .totalElements(chatRoomInfos.size())
                .build();
    }
    public PartyChatRoomDTO.ChatRoomInfo toChatRoomInfo(ChatRoom chatRoom,
                                                        int memberCount,
                                                        int unreadCount,
                                                        PartyChatRoomDTO.LastMessageInfo lastMessageInfo) {
        return PartyChatRoomDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .partyId(chatRoom.getId())
                .partyName(chatRoom.getName())
                .partyImgUrl(getPartyImgUrl(chatRoom))
                .memberCount(memberCount)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageInfo)
                .build();
    }

    public PartyChatRoomDTO.LastMessageInfo toLastMessageInfo(ChatMessage message) {
        if (message == null) return null;

        return PartyChatRoomDTO.LastMessageInfo.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .senderName(message.getSender().getMemberName())
                .timestamp(message.getCreatedAt())
                .messageType(message.getType().name())
                .build();
    }

    private String getPartyImgUrl(ChatRoom chatRoom) {
        if (chatRoom.getParty() != null && chatRoom.getParty().getPartyImg() != null) {
            return imageService.getUrlFromKey(chatRoom.getParty().getPartyImg().getImgKey());
        }
        return null;
    }

}
