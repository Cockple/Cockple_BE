package umc.cockple.demo.domain.chat.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;

public interface ChatImageService {
    ChatDownloadTokenDTO.Response issueDownloadToken(Long imageId, Long memberId);
    ResponseEntity<Resource> downloadImage(Long imageId, String token);
}