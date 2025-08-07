package umc.cockple.demo.domain.chat.service;

import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.enums.Direction;

public interface ChatQueryService {
    PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, int page, int size);

    PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, int page, int size);

    DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, Long cursor, int size, Direction direction);

    DirectChatRoomDTO.Response searchDirectChatRoomsByName(Long memberId, String name, Long cursor, int size, Direction direction);

    ChatRoomDetailDTO.Response getChatRoomDetail(Long roomId, Long memberId);

    ChatMessageDTO.Response getChatMessages(Long roomId, Long memberId, Long cursor, int size);
}
