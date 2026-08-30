package umc.cockple.demo.domain.chat.presentation.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;
import umc.cockple.demo.domain.chat.service.query.PartyChatRoomIdQueryService;
import umc.cockple.demo.domain.chat.service.query.PartyChatRoomQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.support.SecurityContextHelper;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyChatController")
class PartyChatControllerTest {

    private static final Long MEMBER_ID = 10L;

    @InjectMocks
    private PartyChatController partyChatController;

    @Mock
    private PartyChatRoomQueryService partyChatRoomQueryService;
    @Mock
    private PartyChatRoomIdQueryService partyChatRoomIdQueryService;

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHelper.setAuthentication(MEMBER_ID, "테스터");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("모임 채팅방 목록 조회를 query service에 위임한다")
    void getPartyChatRooms_delegatesToQueryService() {
        PartyChatRoomDTO.Response expected = PartyChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(partyChatRoomQueryService.getPartyChatRooms(MEMBER_ID, 1, 5)).willReturn(expected);

        BaseResponse<PartyChatRoomDTO.Response> response = partyChatController.getPartyChatRooms(1, 5);

        assertThat(response.getData()).isSameAs(expected);
        verify(partyChatRoomQueryService).getPartyChatRooms(MEMBER_ID, 1, 5);
    }

    @Test
    @DisplayName("모임 채팅방 검색을 query service에 위임한다")
    void searchPartyChatRooms_delegatesToQueryService() {
        String name = "배드민턴";
        PartyChatRoomDTO.Response expected = PartyChatRoomDTO.Response.builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
        given(partyChatRoomQueryService.searchPartyChatRoomsByName(MEMBER_ID, name, 2, 10)).willReturn(expected);

        BaseResponse<PartyChatRoomDTO.Response> response = partyChatController.searchPartyChatRooms(name, 2, 10);

        assertThat(response.getData()).isSameAs(expected);
        verify(partyChatRoomQueryService).searchPartyChatRoomsByName(MEMBER_ID, name, 2, 10);
    }

    @Test
    @DisplayName("모임 채팅방 ID 조회를 query service에 위임한다")
    void getChatRoomId_delegatesToQueryService() {
        Long partyId = 1L;
        PartyChatRoomIdDTO expected = PartyChatRoomIdDTO.builder().roomId(100L).build();
        given(partyChatRoomIdQueryService.getChatRoomId(partyId, MEMBER_ID)).willReturn(expected);

        BaseResponse<PartyChatRoomIdDTO> response = partyChatController.getChatRoomId(partyId);

        assertThat(response.getData()).isSameAs(expected);
        verify(partyChatRoomIdQueryService).getChatRoomId(partyId, MEMBER_ID);
    }
}
