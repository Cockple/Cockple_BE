package umc.cockple.demo.domain.chat.service;

import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;

public interface ChatFileService {
    ChatDownloadTokenDTO.Response issueDownloadToken(Long fileId, Long memberId);
}
