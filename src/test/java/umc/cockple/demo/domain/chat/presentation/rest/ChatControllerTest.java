package umc.cockple.demo.domain.chat.presentation.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;
import umc.cockple.demo.domain.chat.service.command.DirectChatRoomCommandService;
import umc.cockple.demo.domain.chat.service.file.ChatFileService;
import umc.cockple.demo.domain.chat.service.query.ChatQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.support.SecurityContextHelper;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController")
class ChatControllerTest {

    private static final Long MEMBER_ID = 10L;

    @InjectMocks
    private ChatController chatController;

    @Mock
    private ChatQueryService chatQueryService;
    @Mock
    private DirectChatRoomCommandService directChatRoomCommandService;
    @Mock
    private ChatFileService chatFileService;

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHelper.setAuthentication(MEMBER_ID, "테스터");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("안 읽은 메시지 여부 조회를 query service에 위임한다")
    void getUnreadStatus_delegatesToQueryService() {
        ChatUnreadStatusDTO.Response expected = ChatUnreadStatusDTO.Response.builder()
                .hasUnread(true)
                .hasPartyUnread(true)
                .hasDirectUnread(false)
                .build();
        given(chatQueryService.getUnreadStatus(MEMBER_ID)).willReturn(expected);

        BaseResponse<ChatUnreadStatusDTO.Response> response = chatController.getUnreadStatus();

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).getUnreadStatus(MEMBER_ID);
    }

    @Test
    @DisplayName("모임 채팅방 목록 조회를 query service에 위임한다")
    void getPartyChatRooms_delegatesToQueryService() {
        PartyChatRoomDTO.Response expected = PartyChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(chatQueryService.getPartyChatRooms(MEMBER_ID, 1, 5)).willReturn(expected);

        BaseResponse<PartyChatRoomDTO.Response> response = chatController.getPartyChatRooms(1, 5);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).getPartyChatRooms(MEMBER_ID, 1, 5);
    }

    @Test
    @DisplayName("모임 채팅방 검색을 query service에 위임한다")
    void searchPartyChatRooms_delegatesToQueryService() {
        String name = "배드민턴";
        PartyChatRoomDTO.Response expected = PartyChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(chatQueryService.searchPartyChatRoomsByName(MEMBER_ID, name, 2, 10)).willReturn(expected);

        BaseResponse<PartyChatRoomDTO.Response> response = chatController.searchPartyChatRooms(name, 2, 10);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).searchPartyChatRoomsByName(MEMBER_ID, name, 2, 10);
    }

    @Test
    @DisplayName("개인 채팅방 목록 조회를 query service에 위임한다")
    void getDirectChatRooms_delegatesToQueryService() {
        DirectChatRoomDTO.Response expected = DirectChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(chatQueryService.getDirectChatRooms(MEMBER_ID, 1, 5)).willReturn(expected);

        BaseResponse<DirectChatRoomDTO.Response> response = chatController.getDirectChatRooms(1, 5);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).getDirectChatRooms(MEMBER_ID, 1, 5);
    }

    @Test
    @DisplayName("개인 채팅방 검색을 query service에 위임한다")
    void searchDirectChatRooms_delegatesToQueryService() {
        String name = "상대방";
        DirectChatRoomDTO.Response expected = DirectChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(chatQueryService.searchDirectChatRoomsByName(MEMBER_ID, name, 2, 10)).willReturn(expected);

        BaseResponse<DirectChatRoomDTO.Response> response = chatController.searchDirectChatRooms(name, 2, 10);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).searchDirectChatRoomsByName(MEMBER_ID, name, 2, 10);
    }

    @Test
    @DisplayName("채팅방 상세 조회를 query service에 위임한다")
    void getChatRoomDetail_delegatesToQueryService() {
        Long roomId = 1L;
        ChatRoomDetailDTO.Response expected = ChatRoomDetailDTO.Response.builder()
                .messages(Collections.emptyList())
                .participants(Collections.emptyList())
                .build();
        given(chatQueryService.getChatRoomDetail(roomId, MEMBER_ID)).willReturn(expected);

        BaseResponse<ChatRoomDetailDTO.Response> response = chatController.getChatRoomDetail(roomId);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).getChatRoomDetail(roomId, MEMBER_ID);
    }

    @Test
    @DisplayName("과거 메시지 조회를 query service에 위임한다")
    void getChatMessages_delegatesToQueryService() {
        Long roomId = 1L;
        Long cursor = 100L;
        ChatMessageDTO.Response expected = ChatMessageDTO.Response.builder()
                .messages(Collections.emptyList())
                .hasNext(false)
                .totalElements(0)
                .build();
        given(chatQueryService.getChatMessages(roomId, MEMBER_ID, cursor, 20)).willReturn(expected);

        BaseResponse<ChatMessageDTO.Response> response = chatController.getChatMessages(roomId, cursor, 20);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).getChatMessages(roomId, MEMBER_ID, cursor, 20);
    }

    @Test
    @DisplayName("모임 채팅방 ID 조회를 query service에 위임한다")
    void getChatRoomId_delegatesToQueryService() {
        Long partyId = 1L;
        PartyChatRoomIdDTO expected = PartyChatRoomIdDTO.builder().roomId(100L).build();
        given(chatQueryService.getChatRoomId(partyId, MEMBER_ID)).willReturn(expected);

        BaseResponse<PartyChatRoomIdDTO> response = chatController.getChatRoomId(partyId);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatQueryService).getChatRoomId(partyId, MEMBER_ID);
    }

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
