package umc.cockple.demo.domain.chat.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;

public interface ChatFileService {
    ChatDownloadTokenDTO.Response issueDownloadToken(Long fileId, Long memberId);
    ResponseEntity<Resource> downloadFile(Long fileId, String token);
}
