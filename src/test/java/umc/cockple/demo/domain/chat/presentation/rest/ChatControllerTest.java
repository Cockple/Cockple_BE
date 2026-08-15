package umc.cockple.demo.domain.chat.presentation.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import umc.cockple.demo.domain.chat.dto.ChatFileDownloadDTO;
import umc.cockple.demo.domain.chat.service.command.DirectChatRoomCommandService;
import umc.cockple.demo.domain.chat.service.file.ChatFileService;
import umc.cockple.demo.domain.chat.service.query.ChatQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController")
class ChatControllerTest {

    @InjectMocks
    private ChatController chatController;

    @Mock
    private ChatQueryService chatQueryService;
    @Mock
    private DirectChatRoomCommandService directChatRoomCommandService;
    @Mock
    private ChatFileService chatFileService;

    @Test
    @DisplayName("채팅 파일 다운로드 정보를 HTTP 파일 응답으로 변환한다")
    void downloadFile_createsHttpFileResponse() throws Exception {
        Long fileId = 1L;
        String token = "valid-token";
        byte[] content = new byte[]{1, 2, 3};
        ChatFileDownloadDTO.Response download = new ChatFileDownloadDTO.Response(
                "테스트.webp",
                "image/webp",
                content.length,
                content
        );
        given(chatFileService.downloadFile(fileId, token)).willReturn(download);

        ResponseEntity<Resource> response = chatController.downloadFile(fileId, token);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("테스트.webp");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("image/webp"));
        assertThat(response.getHeaders().getContentLength()).isEqualTo(content.length);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).containsExactly(content);
    }
}
