package umc.cockple.demo.domain.chat.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.enums.Direction;

public interface ChatQueryService {
    PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, Long cursor, int size, Direction direction);

    PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, Long cursor, int size, Direction direction);

    DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, Long cursor, int size, Direction direction);

    DirectChatRoomDTO.Response searchDirectChatRoomsByName(Long memberId, String name, Long cursor, int size, Direction direction);

    ChatRoomDetailDTO.Response getChatRoomDetail(Long roomId, Long memberId);

    Slice<ChatMessageDTO.MessageInfo> getChatMessages(Long roomId, Long memberId, Long cursor, Pageable pageable);
}
