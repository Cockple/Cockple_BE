package umc.cockple.demo.domain.chat.presentation.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.domain.chat.service.query.ChatMessageHistoryQueryService;
import umc.cockple.demo.domain.chat.service.query.ChatRoomDetailQueryService;
import umc.cockple.demo.domain.chat.service.query.ChatUnreadQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.support.SecurityContextHelper;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomController")
class ChatRoomControllerTest {

    private static final Long MEMBER_ID = 10L;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @Mock
    private ChatUnreadQueryService chatUnreadQueryService;
    @Mock
    private ChatRoomDetailQueryService chatRoomDetailQueryService;
    @Mock
    private ChatMessageHistoryQueryService chatMessageHistoryQueryService;

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
        given(chatUnreadQueryService.getUnreadStatus(MEMBER_ID)).willReturn(expected);

        BaseResponse<ChatUnreadStatusDTO.Response> response = chatRoomController.getUnreadStatus();

        assertThat(response.getData()).isSameAs(expected);
        verify(chatUnreadQueryService).getUnreadStatus(MEMBER_ID);
    }

    @Test
    @DisplayName("채팅방 상세 조회를 query service에 위임한다")
    void getChatRoomDetail_delegatesToQueryService() {
        Long roomId = 1L;
        ChatRoomDetailDTO.Response expected = ChatRoomDetailDTO.Response.builder()
                .messages(Collections.emptyList())
                .participants(Collections.emptyList())
                .build();
        given(chatRoomDetailQueryService.getChatRoomDetail(roomId, MEMBER_ID)).willReturn(expected);

        BaseResponse<ChatRoomDetailDTO.Response> response = chatRoomController.getChatRoomDetail(roomId);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatRoomDetailQueryService).getChatRoomDetail(roomId, MEMBER_ID);
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
        given(chatMessageHistoryQueryService.getChatMessages(roomId, MEMBER_ID, cursor, 20)).willReturn(expected);

        BaseResponse<ChatMessageDTO.Response> response = chatRoomController.getChatMessages(roomId, cursor, 20);

        assertThat(response.getData()).isSameAs(expected);
        verify(chatMessageHistoryQueryService).getChatMessages(roomId, MEMBER_ID, cursor, 20);
    }
}
