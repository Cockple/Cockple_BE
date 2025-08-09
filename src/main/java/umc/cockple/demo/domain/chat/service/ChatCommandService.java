package umc.cockple.demo.domain.chat.service;

import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;

public interface ChatCommandService {

    DirectChatRoomCreateDTO.Response createDirectChatRoom(Long memberId, Long targetMemberId);
    ChatDownloadTokenDTO.Response issueDownloadToken(Long memberId, Long fileId);
}
