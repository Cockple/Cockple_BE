package umc.cockple.demo.domain.chat.presentation.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/chats/files")
@ChatApiTag
public interface ChatFileApi {

    @PostMapping("/{fileId}/download-token")
    @Operation(summary = "채팅 파일 다운로드 토큰 발급", description = "채팅방에 업로드된 특정 파일을 다운로드할 수 있는 일회용 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "토큰 발급 성공")
    @ApiResponse(responseCode = "403", description = "파일 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 파일")
    BaseResponse<ChatDownloadTokenDTO.Response> issueDownloadToken(
            @PathVariable Long fileId
    );

    @GetMapping("/{fileId}/download")
    @Operation(summary = "채팅 파일 다운로드", description = "발급받은 다운로드 토큰을 검증하고, 유효할 경우 실제 파일 데이터를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "파일 다운로드 성공")
    @ApiResponse(responseCode = "403", description = "유효하지 않거나 만료된 토큰")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 파일")
    ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam String token
    );
}
