package umc.cockple.demo.domain.chat.service;

import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;

public interface ChatQueryService {
    PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, Long cursor, int size, String direction);
}
