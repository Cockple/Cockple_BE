package umc.cockple.demo.domain.chat.service.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.LastMessageCacheDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatRoomUnreadCountDTO;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyChatRoomQueryServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomListCacheService chatRoomListCacheService;
    @Mock private FileService fileService;

    private ChatConverter chatConverter;
    private ChatUnreadQueryService chatUnreadQueryService;
    private ImageUrlResolver imageUrlResolver;
    private PartyChatRoomQueryService partyChatRoomQueryService;

    @BeforeEach
    void setUp() {
        chatConverter = new ChatConverter();
        chatUnreadQueryService = new ChatUnreadQueryService(messageReadStatusRepository);
        imageUrlResolver = new ImageUrlResolver(fileService);
        partyChatRoomQueryService = new PartyChatRoomQueryService(
                chatRoomRepository,
                chatRoomMemberRepository,
                chatUnreadQueryService,
                chatConverter,
                imageUrlResolver,
                chatRoomListCacheService
        );
        lenient().when(messageReadStatusRepository.countUnreadMessagesByChatRooms(anyLong(), anyList()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("getPartyChatRooms - 모임 채팅방 목록 조회")
    class GetPartyChatRooms {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("채팅방이 없으면 빈 목록과 hasNext false를 반환한다")
            void emptySlice_returnsEmptyResponse() {
                // given
                Long memberId = 10L;
                Slice<ChatRoom> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(emptySlice);

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.getPartyChatRooms(memberId, 0, 10);

                // then
                assertThat(result.content()).isEmpty();
                assertThat(result.hasNext()).isFalse();
            }

            @Test
            @DisplayName("마지막으로 읽은 메시지가 없으면 전체 미읽음 개수를 사용한다")
            void unreadCount_usesAllUnreadMessages_whenLastReadMessageIdIsNull() {
                // given
                Long memberId = 10L;
                Long roomId = 1L;

                Party party = PartyFixture.createParty("배드민턴 모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
                ReflectionTestUtils.setField(party, "id", 100L);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);
                given(messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, List.of(roomId)))
                        .willReturn(List.of(new ChatRoomUnreadCountDTO(roomId, 4L)));

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.getPartyChatRooms(memberId, 0, 10);

                // then
                assertThat(result.hasNext()).isFalse();
                assertThat(result.content()).hasSize(1);
                PartyChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
                assertThat(roomInfo.partyId()).isEqualTo(100L);
                assertThat(roomInfo.partyName()).isEqualTo("배드민턴 모임");
                assertThat(roomInfo.memberCount()).isEqualTo(1);
                assertThat(roomInfo.unreadCount()).isEqualTo(4);
                assertThat(roomInfo.partyImgUrl()).isNull();
                assertThat(roomInfo.lastMessage()).isNull();
                verify(messageReadStatusRepository).countUnreadMessagesByChatRooms(memberId, List.of(roomId));
                verify(chatRoomMemberRepository, never()).findByChatRoomIdAndMemberId(roomId, memberId);
            }

            @Test
            @DisplayName("마지막으로 읽은 메시지가 있으면 이후 미읽음 개수를 사용하고 마지막 메시지와 이미지 URL을 매핑한다")
            void mapsLastMessageAndPartyImage_andUsesUnreadAfter_whenLastReadMessageExists() {
                // given
                Long memberId = 10L;
                Long roomId = 2L;
                LocalDateTime sentAt = LocalDateTime.of(2026, 4, 1, 12, 30);

                Party party = PartyFixture.createParty("아침 배드민턴", memberId, PartyFixture.createPartyAddr("서울", "송파구"));
                ReflectionTestUtils.setField(party, "id", 200L);
                PartyImg partyImg = PartyImg.create("party/image.png", party);
                ReflectionTestUtils.setField(party, "partyImg", partyImg);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 5), true);
                LastMessageCacheDTO lastMessage = LastMessageCacheDTO.builder()
                        .content("최근 공지")
                        .timestamp(sentAt)
                        .messageType("TEXT")
                        .build();

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 5)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(3);
                given(messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, List.of(roomId)))
                        .willReturn(List.of(new ChatRoomUnreadCountDTO(roomId, 2L)));
                given(chatRoomListCacheService.getLastMessage(roomId)).willReturn(lastMessage);
                given(fileService.getUrlFromKey("party/image.png")).willReturn("https://cdn.example.com/party/image.png");

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.getPartyChatRooms(memberId, 0, 5);

                // then
                assertThat(result.hasNext()).isTrue();
                assertThat(result.content()).hasSize(1);

                PartyChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
                assertThat(roomInfo.partyId()).isEqualTo(200L);
                assertThat(roomInfo.partyName()).isEqualTo("아침 배드민턴");
                assertThat(roomInfo.memberCount()).isEqualTo(3);
                assertThat(roomInfo.unreadCount()).isEqualTo(2);
                assertThat(roomInfo.partyImgUrl()).isEqualTo("https://cdn.example.com/party/image.png");
                assertThat(roomInfo.lastMessage()).isNotNull();
                assertThat(roomInfo.lastMessage().content()).isEqualTo("최근 공지");
                assertThat(roomInfo.lastMessage().timestamp()).isEqualTo(sentAt);
                assertThat(roomInfo.lastMessage().messageType()).isEqualTo("TEXT");

                verify(messageReadStatusRepository).countUnreadMessagesByChatRooms(memberId, List.of(roomId));
                verify(chatRoomListCacheService).getLastMessage(roomId);
                verify(fileService).getUrlFromKey("party/image.png");
            }

            @Test
            @DisplayName("채팅방 목록은 최신 메시지 기준으로 받은 순서를 유지한다")
            void preservesLatestMessageFirstOrder() {
                // given
                Long memberId = 10L;

                Party newerParty = PartyFixture.createParty("최근 모임", memberId, PartyFixture.createPartyAddr("서울", "강동구"));
                ReflectionTestUtils.setField(newerParty, "id", 401L);
                ChatRoom newerRoom = ChatFixture.createPartyChatRoom(newerParty);
                ReflectionTestUtils.setField(newerRoom, "id", 11L);
                Party olderParty = PartyFixture.createParty("이전 모임", memberId, PartyFixture.createPartyAddr("서울", "서초구"));
                ReflectionTestUtils.setField(olderParty, "id", 402L);
                ChatRoom olderRoom = ChatFixture.createPartyChatRoom(olderParty);
                ReflectionTestUtils.setField(olderRoom, "id", 12L);
                Slice<ChatRoom> orderedChatRooms = new SliceImpl<>(List.of(newerRoom, olderRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(orderedChatRooms);
                given(chatRoomMemberRepository.countByChatRoomId(11L)).willReturn(2);
                given(chatRoomMemberRepository.countByChatRoomId(12L)).willReturn(2);
                given(chatRoomListCacheService.getLastMessage(11L)).willReturn(
                        LastMessageCacheDTO.builder().content("가장 최근 메시지").timestamp(LocalDateTime.of(2026, 4, 1, 20, 0)).messageType("TEXT").build());
                given(chatRoomListCacheService.getLastMessage(12L)).willReturn(
                        LastMessageCacheDTO.builder().content("이전 메시지").timestamp(LocalDateTime.of(2026, 4, 1, 19, 0)).messageType("TEXT").build());

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.getPartyChatRooms(memberId, 0, 10);

                // then
                assertThat(result.content()).hasSize(2);
                assertThat(result.content().get(0).chatRoomId()).isEqualTo(11L);
                assertThat(result.content().get(0).partyName()).isEqualTo("최근 모임");
                assertThat(result.content().get(0).lastMessage().content()).isEqualTo("가장 최근 메시지");
                assertThat(result.content().get(1).chatRoomId()).isEqualTo(12L);
                assertThat(result.content().get(1).partyName()).isEqualTo("이전 모임");
                assertThat(result.content().get(1).lastMessage().content()).isEqualTo("이전 메시지");
            }
        }

    }

    @Nested
    @DisplayName("searchPartyChatRoomsByName - 모임 채팅방 이름 검색")
    class SearchPartyChatRoomsByName {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("검색 결과가 없으면 빈 목록과 hasNext false를 반환한다")
            void emptySlice_returnsEmptyResponse() {
                // given
                Long memberId = 10L;
                String name = "배드";
                Slice<ChatRoom> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchPartyChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(emptySlice);

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10);

                // then
                assertThat(result.content()).isEmpty();
                assertThat(result.hasNext()).isFalse();
            }

            @Test
            @DisplayName("검색된 모임 채팅방만 응답에 유지하고 partyName을 반환한다")
            void returnsOnlyMatchedRoom_whenRepositoryAlreadyFilteredResults() {
                // given
                Long memberId = 10L;
                Long roomId = 21L;
                String name = "배드";

                Party party = PartyFixture.createParty("배드민턴 모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
                ReflectionTestUtils.setField(party, "id", 501L);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                Slice<ChatRoom> searchResult = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchPartyChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(searchResult);
                given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10);

                // then
                assertThat(result.hasNext()).isFalse();
                assertThat(result.content()).hasSize(1);
                PartyChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
                assertThat(roomInfo.partyId()).isEqualTo(501L);
                assertThat(roomInfo.partyName()).isEqualTo("배드민턴 모임");
                assertThat(roomInfo.memberCount()).isEqualTo(1);
                assertThat(roomInfo.unreadCount()).isEqualTo(0);
                assertThat(roomInfo.partyImgUrl()).isNull();
                assertThat(roomInfo.lastMessage()).isNull();
            }

            @Test
            @DisplayName("검색 결과는 repository가 반환한 최신 메시지 순서를 그대로 유지한다")
            void preservesRepositoryOrder() {
                // given
                Long memberId = 10L;
                String name = "배드";

                Party newerParty = PartyFixture.createParty("배드민턴 새벽 모임", memberId, PartyFixture.createPartyAddr("서울", "강동구"));
                ReflectionTestUtils.setField(newerParty, "id", 601L);
                ChatRoom newerRoom = ChatFixture.createPartyChatRoom(newerParty);
                ReflectionTestUtils.setField(newerRoom, "id", 31L);
                Party olderParty = PartyFixture.createParty("배드민턴 저녁 모임", memberId, PartyFixture.createPartyAddr("서울", "서초구"));
                ReflectionTestUtils.setField(olderParty, "id", 602L);
                ChatRoom olderRoom = ChatFixture.createPartyChatRoom(olderParty);
                ReflectionTestUtils.setField(olderRoom, "id", 32L);
                Slice<ChatRoom> orderedRooms = new SliceImpl<>(List.of(newerRoom, olderRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchPartyChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(orderedRooms);
                given(chatRoomMemberRepository.countByChatRoomId(31L)).willReturn(2);
                given(chatRoomMemberRepository.countByChatRoomId(32L)).willReturn(2);

                // when
                PartyChatRoomDTO.Response result = partyChatRoomQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10);

                // then
                assertThat(result.content()).hasSize(2);
                assertThat(result.content().get(0).chatRoomId()).isEqualTo(31L);
                assertThat(result.content().get(0).partyName()).isEqualTo("배드민턴 새벽 모임");
                assertThat(result.content().get(1).chatRoomId()).isEqualTo(32L);
                assertThat(result.content().get(1).partyName()).isEqualTo("배드민턴 저녁 모임");
            }
        }

    }
}
