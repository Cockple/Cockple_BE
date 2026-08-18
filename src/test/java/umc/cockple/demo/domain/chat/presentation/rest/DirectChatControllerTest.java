package umc.cockple.demo.domain.chat.presentation.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.service.command.DirectChatRoomCommandService;
import umc.cockple.demo.domain.chat.service.query.DirectChatRoomQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.support.SecurityContextHelper;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectChatController")
class DirectChatControllerTest {

    private static final Long MEMBER_ID = 10L;

    @InjectMocks
    private DirectChatController directChatController;

    @Mock
    private DirectChatRoomCommandService directChatRoomCommandService;
    @Mock
    private DirectChatRoomQueryService directChatRoomQueryService;

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHelper.setAuthentication(MEMBER_ID, "테스터");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("개인 채팅방 생성을 command service에 위임한다")
    void createDirectChatRoom_delegatesToCommandService() {
        Long targetMemberId = 20L;
        DirectChatRoomCreateDTO.Response expected = DirectChatRoomCreateDTO.Response.builder()
                .chatRoomId(100L)
                .members(Collections.emptyList())
                .build();
        given(directChatRoomCommandService.createDirectChatRoom(MEMBER_ID, targetMemberId)).willReturn(expected);

        BaseResponse<DirectChatRoomCreateDTO.Response> response =
                directChatController.createDirectChatRoom(targetMemberId);

        assertThat(response.getData()).isSameAs(expected);
        verify(directChatRoomCommandService).createDirectChatRoom(MEMBER_ID, targetMemberId);
    }

    @Test
    @DisplayName("개인 채팅방 목록 조회를 query service에 위임한다")
    void getDirectChatRooms_delegatesToQueryService() {
        DirectChatRoomDTO.Response expected = DirectChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(directChatRoomQueryService.getDirectChatRooms(MEMBER_ID, 1, 5)).willReturn(expected);

        BaseResponse<DirectChatRoomDTO.Response> response = directChatController.getDirectChatRooms(1, 5);

        assertThat(response.getData()).isSameAs(expected);
        verify(directChatRoomQueryService).getDirectChatRooms(MEMBER_ID, 1, 5);
    }

    @Test
    @DisplayName("개인 채팅방 검색을 query service에 위임한다")
    void searchDirectChatRooms_delegatesToQueryService() {
        String name = "상대방";
        DirectChatRoomDTO.Response expected = DirectChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(directChatRoomQueryService.searchDirectChatRoomsByName(MEMBER_ID, name, 2, 10)).willReturn(expected);

        BaseResponse<DirectChatRoomDTO.Response> response = directChatController.searchDirectChatRooms(name, 2, 10);

        assertThat(response.getData()).isSameAs(expected);
        verify(directChatRoomQueryService).searchDirectChatRoomsByName(MEMBER_ID, name, 2, 10);
    }
}
