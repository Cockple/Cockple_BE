package umc.cockple.demo.domain.chat.service.command;

import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;

public interface ChatCommandService {

    DirectChatRoomCreateDTO.Response createDirectChatRoom(Long memberId, Long targetMemberId);
}
