package umc.cockple.demo.domain.chat.presentation.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.domain.chat.dto.ChatFileDownloadDTO;
import umc.cockple.demo.domain.chat.presentation.rest.api.ChatFileApi;
import umc.cockple.demo.domain.chat.service.file.ChatFileService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@Validated
public class ChatFileController implements ChatFileApi {

    private final ChatFileService chatFileService;

    @Override
    public BaseResponse<ChatDownloadTokenDTO.Response> issueDownloadToken(Long fileId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatDownloadTokenDTO.Response response = chatFileService.issueDownloadToken(fileId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long fileId, String token) {
        ChatFileDownloadDTO.Response response = chatFileService.downloadFile(fileId, token);
        return createDownloadResponse(response);
    }

    private ResponseEntity<Resource> createDownloadResponse(ChatFileDownloadDTO.Response response) {
        Resource resource = new InputStreamResource(new ByteArrayInputStream(response.content()));
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(response.originalFileName(), StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.parseMediaType(response.contentType()));
        headers.setContentLength(response.contentLength());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
