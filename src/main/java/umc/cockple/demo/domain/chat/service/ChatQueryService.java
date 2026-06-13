package umc.cockple.demo.domain.chat.service;

import umc.cockple.demo.domain.chat.dto.*;

public interface ChatQueryService {
    PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, int page, int size);

    PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, int page, int size);

    DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, int page, int size);

    DirectChatRoomDTO.Response searchDirectChatRoomsByName(Long memberId, String name, int page, int size);

    ChatUnreadSummaryDTO.Response getUnreadSummary(Long memberId);

    ChatUnreadCountDTO.Response getPartyUnreadCount(Long memberId);

    ChatUnreadCountDTO.Response getDirectUnreadCount(Long memberId);

    ChatRoomDetailDTO.Response getChatRoomDetail(Long roomId, Long memberId);

    ChatMessageDTO.Response getChatMessages(Long roomId, Long memberId, Long cursor, int size);

    PartyChatRoomIdDTO getChatRoomId(Long partyId, Long memberId);
}
