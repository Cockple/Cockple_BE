package umc.cockple.demo.domain.chat.service;

import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.enums.Direction;

public interface ChatQueryService {
    PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, Long cursor, int size, Direction direction);

    PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, Long cursor, int size, Direction direction);

}
