package umc.cockple.demo.domain.chat.service.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    @Mock private PartyChatRoomQueryService partyChatRoomQueryService;
    @Mock private DirectChatRoomQueryService directChatRoomQueryService;
    @Mock private ChatUnreadQueryService chatUnreadQueryService;
    @Mock private ChatRoomDetailQueryService chatRoomDetailQueryService;
    @Mock private ChatMessageHistoryQueryService chatMessageHistoryQueryService;
    @Mock private PartyChatRoomIdQueryService partyChatRoomIdQueryService;

    private ChatQueryServiceImpl chatQueryService;

    @BeforeEach
    void setUp() {
        chatQueryService = new ChatQueryServiceImpl(
                partyChatRoomQueryService,
                directChatRoomQueryService,
                chatUnreadQueryService,
                chatRoomDetailQueryService,
                chatMessageHistoryQueryService,
                partyChatRoomIdQueryService
        );
    }

    @Nested
    @DisplayName("과거 메시지 조회 위임")
    class ChatMessageHistoryDelegation {

        @Test
        @DisplayName("과거 메시지 조회는 전용 조회 서비스로 위임한다")
        void delegatesChatMessageHistory() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;
            int size = 20;
            ChatMessageDTO.Response expected = ChatMessageDTO.Response.builder()
                    .messages(Collections.emptyList())
                    .hasNext(false)
                    .nextCursor(null)
                    .totalElements(0)
                    .build();
            given(chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size))
                    .willReturn(expected);

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, size);

            // then
            assertThat(result).isSameAs(expected);
            verify(chatMessageHistoryQueryService).getChatMessages(roomId, memberId, cursor, size);
        }
    }

    @Nested
    @DisplayName("모임 채팅방 ID 조회 위임")
    class PartyChatRoomIdDelegation {

        @Test
        @DisplayName("모임 채팅방 ID 조회는 전용 조회 서비스로 위임한다")
        void delegatesPartyChatRoomId() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;
            PartyChatRoomIdDTO expected = PartyChatRoomIdDTO.builder().roomId(100L).build();
            given(partyChatRoomIdQueryService.getChatRoomId(partyId, memberId)).willReturn(expected);

            // when
            PartyChatRoomIdDTO result = chatQueryService.getChatRoomId(partyId, memberId);

            // then
            assertThat(result).isSameAs(expected);
            verify(partyChatRoomIdQueryService).getChatRoomId(partyId, memberId);
        }
    }

    @Nested
    @DisplayName("안 읽은 메시지 여부 조회 위임")
    class GetUnreadStatusDelegation {

        @Test
        @DisplayName("안읽음 여부 조회는 unread 조회 서비스로 위임한다")
        void delegatesUnreadStatus() {
            // given
            Long memberId = 10L;
            ChatUnreadStatusDTO.Response expected = ChatUnreadStatusDTO.Response.builder()
                    .hasUnread(true)
                    .hasPartyUnread(true)
                    .hasDirectUnread(false)
                    .build();
            given(chatUnreadQueryService.getUnreadStatus(memberId)).willReturn(expected);

            // when
            ChatUnreadStatusDTO.Response result = chatQueryService.getUnreadStatus(memberId);

            // then
            assertThat(result).isSameAs(expected);
            verify(chatUnreadQueryService).getUnreadStatus(memberId);
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 위임")
    class ChatRoomListDelegation {

        @Test
        @DisplayName("모임 채팅방 목록 조회는 모임 조회 서비스로 위임한다")
        void delegatesPartyChatRooms() {
            // given
            Long memberId = 10L;
            PartyChatRoomDTO.Response expected = PartyChatRoomDTO.Response.builder()
                    .content(Collections.emptyList())
                    .hasNext(false)
                    .build();
            given(partyChatRoomQueryService.getPartyChatRooms(memberId, 0, 10))
                    .willReturn(expected);

            // when
            PartyChatRoomDTO.Response result = chatQueryService.getPartyChatRooms(memberId, 0, 10);

            // then
            assertThat(result).isSameAs(expected);
            verify(partyChatRoomQueryService).getPartyChatRooms(memberId, 0, 10);
        }

        @Test
        @DisplayName("모임 채팅방 검색은 모임 조회 서비스로 위임한다")
        void delegatesPartyChatRoomSearch() {
            // given
            Long memberId = 10L;
            String name = "배드";
            PartyChatRoomDTO.Response expected = PartyChatRoomDTO.Response.builder()
                    .content(Collections.emptyList())
                    .hasNext(false)
                    .build();
            given(partyChatRoomQueryService.searchPartyChatRoomsByName(memberId, name, 1, 5))
                    .willReturn(expected);

            // when
            PartyChatRoomDTO.Response result = chatQueryService.searchPartyChatRoomsByName(memberId, name, 1, 5);

            // then
            assertThat(result).isSameAs(expected);
            verify(partyChatRoomQueryService).searchPartyChatRoomsByName(memberId, name, 1, 5);
        }

        @Test
        @DisplayName("개인 채팅방 목록 조회는 개인 조회 서비스로 위임한다")
        void delegatesDirectChatRooms() {
            // given
            Long memberId = 10L;
            DirectChatRoomDTO.Response expected = DirectChatRoomDTO.Response.builder()
                    .content(Collections.emptyList())
                    .hasNext(false)
                    .build();
            given(directChatRoomQueryService.getDirectChatRooms(memberId, 0, 10))
                    .willReturn(expected);

            // when
            DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 10);

            // then
            assertThat(result).isSameAs(expected);
            verify(directChatRoomQueryService).getDirectChatRooms(memberId, 0, 10);
        }

        @Test
        @DisplayName("개인 채팅방 검색은 개인 조회 서비스로 위임한다")
        void delegatesDirectChatRoomSearch() {
            // given
            Long memberId = 10L;
            String name = "영희";
            DirectChatRoomDTO.Response expected = DirectChatRoomDTO.Response.builder()
                    .content(Collections.emptyList())
                    .hasNext(false)
                    .build();
            given(directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, 1, 5))
                    .willReturn(expected);

            // when
            DirectChatRoomDTO.Response result = chatQueryService.searchDirectChatRoomsByName(memberId, name, 1, 5);

            // then
            assertThat(result).isSameAs(expected);
            verify(directChatRoomQueryService).searchDirectChatRoomsByName(memberId, name, 1, 5);
        }
    }

    @Nested
    @DisplayName("채팅방 상세 조회 위임")
    class ChatRoomDetailDelegation {

        @Test
        @DisplayName("채팅방 상세 조회는 전용 조회 서비스로 위임한다")
        void delegatesChatRoomDetail() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            ChatRoomDetailDTO.Response expected = ChatRoomDetailDTO.Response.builder()
                    .chatRoomInfo(null)
                    .messages(Collections.emptyList())
                    .participants(Collections.emptyList())
                    .build();
            given(chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId)).willReturn(expected);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result).isSameAs(expected);
            verify(chatRoomDetailQueryService).getChatRoomDetail(roomId, memberId);
        }
    }
}
