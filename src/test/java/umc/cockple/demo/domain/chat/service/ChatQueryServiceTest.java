package umc.cockple.demo.domain.chat.service;

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
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.LastMessageCacheDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    // Mocks (외부 I/O)
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomListCacheService chatRoomListCacheService;
    @Mock private FileService fileService;

    private ChatConverter chatConverter;
    private ChatProcessor chatProcessor;
    private ChatUnreadQueryService chatUnreadQueryService;
    private ChatQueryServiceImpl chatQueryService;

    @BeforeEach
    void setUp() {
        chatConverter = new ChatConverter();
        chatProcessor = new ChatProcessor(fileService, chatConverter);
        chatUnreadQueryService = new ChatUnreadQueryService(messageReadStatusRepository, chatRoomMemberRepository);
        chatQueryService = new ChatQueryServiceImpl(
                chatRoomRepository,
                chatRoomMemberRepository,
                chatMessageRepository,
                partyRepository,
                memberPartyRepository,
                chatUnreadQueryService,
                chatConverter,
                fileService,
                chatProcessor,
                chatRoomListCacheService
        );
    }

    @Nested
    @DisplayName("안 읽은 메시지 수 조회")
    class GetUnreadCounts {

        @Test
        @DisplayName("요약 조회는 모임과 개인 안읽음 수를 합산한다")
        void getUnreadSummary_addsPartyAndDirectUnreadCounts() {
            // given
            Long memberId = 10L;
            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            ChatRoom partyRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(partyRoom, "id", 1L);
            ChatRoomMember partyMembership = ChatFixture.createJoinedMember(partyRoom, me);

            ChatRoom directRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(directRoom, "id", 2L);
            ChatRoomMember directMembership = ChatFixture.createJoinedMemberWithLastRead(directRoom, me, 30L);

            given(chatRoomMemberRepository.findPartyChatRoomMembersByMemberId(memberId)).willReturn(List.of(partyMembership));
            given(chatRoomMemberRepository.findJoinedDirectChatRoomMembersByMemberId(memberId)).willReturn(List.of(directMembership));
            given(messageReadStatusRepository.countAllUnreadMessages(1L, memberId)).willReturn(3);
            given(messageReadStatusRepository.countUnreadMessagesAfter(2L, memberId, 30L)).willReturn(2);

            // when
            var result = chatQueryService.getUnreadSummary(memberId);

            // then
            assertThat(result.partyUnreadCount()).isEqualTo(3);
            assertThat(result.directUnreadCount()).isEqualTo(2);
            assertThat(result.totalUnreadCount()).isEqualTo(5);
            assertThat(result.hasUnread()).isTrue();
        }

        @Test
        @DisplayName("모임 안읽음 수가 0이면 hasUnread false를 반환한다")
        void getPartyUnreadCount_returnsFalseWhenZero() {
            // given
            Long memberId = 10L;
            given(chatRoomMemberRepository.findPartyChatRoomMembersByMemberId(memberId)).willReturn(List.of());

            // when
            var result = chatQueryService.getPartyUnreadCount(memberId);

            // then
            assertThat(result.unreadCount()).isZero();
            assertThat(result.hasUnread()).isFalse();
        }

        @Test
        @DisplayName("개인 안읽음 수가 있으면 hasUnread true를 반환한다")
        void getDirectUnreadCount_returnsTrueWhenUnreadExists() {
            // given
            Long memberId = 10L;
            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            ChatRoom directRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(directRoom, "id", 1L);
            ChatRoomMember directMembership = ChatFixture.createJoinedMember(directRoom, me);

            given(chatRoomMemberRepository.findJoinedDirectChatRoomMembersByMemberId(memberId)).willReturn(List.of(directMembership));
            given(messageReadStatusRepository.countAllUnreadMessages(1L, memberId)).willReturn(7);

            // when
            var result = chatQueryService.getDirectUnreadCount(memberId);

            // then
            assertThat(result.unreadCount()).isEqualTo(7);
            assertThat(result.hasUnread()).isTrue();
        }
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
                PartyChatRoomDTO.Response result = chatQueryService.getPartyChatRooms(memberId, 0, 10);

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

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Party party = PartyFixture.createParty("배드민턴 모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
                ReflectionTestUtils.setField(party, "id", 100L);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember membership = ChatFixture.createJoinedMember(chatRoom, me);
                ReflectionTestUtils.setField(membership, "id", 1L);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(membership));
                given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);
                given(messageReadStatusRepository.countAllUnreadMessages(roomId, memberId)).willReturn(4);

                // when
                PartyChatRoomDTO.Response result = chatQueryService.getPartyChatRooms(memberId, 0, 10);

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
                verify(messageReadStatusRepository).countAllUnreadMessages(roomId, memberId);
                verify(messageReadStatusRepository, never()).countUnreadMessagesAfter(anyLong(), anyLong(), anyLong());
            }

            @Test
            @DisplayName("마지막으로 읽은 메시지가 있으면 이후 미읽음 개수를 사용하고 마지막 메시지와 이미지 URL을 매핑한다")
            void mapsLastMessageAndPartyImage_andUsesUnreadAfter_whenLastReadMessageExists() {
                // given
                Long memberId = 10L;
                Long roomId = 2L;
                LocalDateTime sentAt = LocalDateTime.of(2026, 4, 1, 12, 30);

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Party party = PartyFixture.createParty("아침 배드민턴", memberId, PartyFixture.createPartyAddr("서울", "송파구"));
                ReflectionTestUtils.setField(party, "id", 200L);
                PartyImg partyImg = PartyImg.create("party/image.png", party);
                ReflectionTestUtils.setField(party, "partyImg", partyImg);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember membership = ChatFixture.createJoinedMemberWithLastRead(chatRoom, me, 30L);
                ReflectionTestUtils.setField(membership, "id", 2L);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 5), true);
                LastMessageCacheDTO lastMessage = LastMessageCacheDTO.builder()
                        .content("최근 공지")
                        .timestamp(sentAt)
                        .messageType("TEXT")
                        .build();

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 5)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(membership));
                given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(3);
                given(messageReadStatusRepository.countUnreadMessagesAfter(roomId, memberId, 30L)).willReturn(2);
                given(chatRoomListCacheService.getLastMessage(roomId)).willReturn(lastMessage);
                given(fileService.getUrlFromKey("party/image.png")).willReturn("https://cdn.example.com/party/image.png");

                // when
                PartyChatRoomDTO.Response result = chatQueryService.getPartyChatRooms(memberId, 0, 5);

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

                verify(messageReadStatusRepository).countUnreadMessagesAfter(roomId, memberId, 30L);
                verify(messageReadStatusRepository, never()).countAllUnreadMessages(roomId, memberId);
                verify(chatRoomListCacheService).getLastMessage(roomId);
                verify(fileService).getUrlFromKey("party/image.png");
            }

            @Test
            @DisplayName("채팅방 목록은 최신 메시지 기준으로 받은 순서를 유지한다")
            void preservesLatestMessageFirstOrder() {
                // given
                Long memberId = 10L;

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Party newerParty = PartyFixture.createParty("최근 모임", memberId, PartyFixture.createPartyAddr("서울", "강동구"));
                ReflectionTestUtils.setField(newerParty, "id", 401L);
                ChatRoom newerRoom = ChatFixture.createPartyChatRoom(newerParty);
                ReflectionTestUtils.setField(newerRoom, "id", 11L);
                ChatRoomMember newerMembership = ChatFixture.createJoinedMember(newerRoom, me);
                ReflectionTestUtils.setField(newerMembership, "id", 11L);

                Party olderParty = PartyFixture.createParty("이전 모임", memberId, PartyFixture.createPartyAddr("서울", "서초구"));
                ReflectionTestUtils.setField(olderParty, "id", 402L);
                ChatRoom olderRoom = ChatFixture.createPartyChatRoom(olderParty);
                ReflectionTestUtils.setField(olderRoom, "id", 12L);
                ChatRoomMember olderMembership = ChatFixture.createJoinedMember(olderRoom, me);
                ReflectionTestUtils.setField(olderMembership, "id", 12L);

                Slice<ChatRoom> orderedChatRooms = new SliceImpl<>(List.of(newerRoom, olderRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(orderedChatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(11L, memberId)).willReturn(Optional.of(newerMembership));
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(12L, memberId)).willReturn(Optional.of(olderMembership));
                given(chatRoomMemberRepository.countByChatRoomId(11L)).willReturn(2);
                given(chatRoomMemberRepository.countByChatRoomId(12L)).willReturn(2);
                given(messageReadStatusRepository.countAllUnreadMessages(11L, memberId)).willReturn(0);
                given(messageReadStatusRepository.countAllUnreadMessages(12L, memberId)).willReturn(0);
                given(chatRoomListCacheService.getLastMessage(11L)).willReturn(
                        LastMessageCacheDTO.builder().content("가장 최근 메시지").timestamp(LocalDateTime.of(2026, 4, 1, 20, 0)).messageType("TEXT").build());
                given(chatRoomListCacheService.getLastMessage(12L)).willReturn(
                        LastMessageCacheDTO.builder().content("이전 메시지").timestamp(LocalDateTime.of(2026, 4, 1, 19, 0)).messageType("TEXT").build());

                // when
                PartyChatRoomDTO.Response result = chatQueryService.getPartyChatRooms(memberId, 0, 10);

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

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("조회된 채팅방의 멤버가 아니면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
            void throwsAccessDenied_whenMembershipIsMissing() {
                // given
                Long memberId = 10L;
                Long roomId = 3L;

                Party party = PartyFixture.createParty("저녁 배드민턴", memberId, PartyFixture.createPartyAddr("서울", "마포구"));
                ReflectionTestUtils.setField(party, "id", 300L);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatQueryService.getPartyChatRooms(memberId, 0, 10))
                        .isInstanceOf(ChatException.class)
                        .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
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
                PartyChatRoomDTO.Response result = chatQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10);

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

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Party party = PartyFixture.createParty("배드민턴 모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
                ReflectionTestUtils.setField(party, "id", 501L);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember membership = ChatFixture.createJoinedMember(chatRoom, me);
                ReflectionTestUtils.setField(membership, "id", 21L);

                Slice<ChatRoom> searchResult = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchPartyChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(searchResult);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(membership));
                given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);
                given(messageReadStatusRepository.countAllUnreadMessages(roomId, memberId)).willReturn(0);

                // when
                PartyChatRoomDTO.Response result = chatQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10);

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

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Party newerParty = PartyFixture.createParty("배드민턴 새벽 모임", memberId, PartyFixture.createPartyAddr("서울", "강동구"));
                ReflectionTestUtils.setField(newerParty, "id", 601L);
                ChatRoom newerRoom = ChatFixture.createPartyChatRoom(newerParty);
                ReflectionTestUtils.setField(newerRoom, "id", 31L);
                ChatRoomMember newerMembership = ChatFixture.createJoinedMember(newerRoom, me);
                ReflectionTestUtils.setField(newerMembership, "id", 31L);

                Party olderParty = PartyFixture.createParty("배드민턴 저녁 모임", memberId, PartyFixture.createPartyAddr("서울", "서초구"));
                ReflectionTestUtils.setField(olderParty, "id", 602L);
                ChatRoom olderRoom = ChatFixture.createPartyChatRoom(olderParty);
                ReflectionTestUtils.setField(olderRoom, "id", 32L);
                ChatRoomMember olderMembership = ChatFixture.createJoinedMember(olderRoom, me);
                ReflectionTestUtils.setField(olderMembership, "id", 32L);

                Slice<ChatRoom> orderedRooms = new SliceImpl<>(List.of(newerRoom, olderRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchPartyChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(orderedRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(31L, memberId)).willReturn(Optional.of(newerMembership));
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(32L, memberId)).willReturn(Optional.of(olderMembership));
                given(chatRoomMemberRepository.countByChatRoomId(31L)).willReturn(2);
                given(chatRoomMemberRepository.countByChatRoomId(32L)).willReturn(2);
                given(messageReadStatusRepository.countAllUnreadMessages(31L, memberId)).willReturn(0);
                given(messageReadStatusRepository.countAllUnreadMessages(32L, memberId)).willReturn(0);

                // when
                PartyChatRoomDTO.Response result = chatQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10);

                // then
                assertThat(result.content()).hasSize(2);
                assertThat(result.content().get(0).chatRoomId()).isEqualTo(31L);
                assertThat(result.content().get(0).partyName()).isEqualTo("배드민턴 새벽 모임");
                assertThat(result.content().get(1).chatRoomId()).isEqualTo(32L);
                assertThat(result.content().get(1).partyName()).isEqualTo("배드민턴 저녁 모임");
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("조회된 검색 결과의 채팅방 멤버가 아니면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
            void throwsAccessDenied_whenMembershipIsMissing() {
                // given
                Long memberId = 10L;
                String name = "배드";
                Long roomId = 33L;

                Party party = PartyFixture.createParty("배드민턴 점심 모임", memberId, PartyFixture.createPartyAddr("서울", "마포구"));
                ReflectionTestUtils.setField(party, "id", 603L);

                ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                Slice<ChatRoom> searchResult = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchPartyChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(searchResult);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatQueryService.searchPartyChatRoomsByName(memberId, name, 0, 10))
                        .isInstanceOf(ChatException.class)
                        .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
            }
        }
    }

    @Nested
    @DisplayName("getDirectChatRooms - 개인 채팅방 목록 조회")
    class GetDirectChatRooms {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("채팅방이 없으면 빈 목록과 hasNext false를 반환한다")
            void emptySlice_returnsEmptyResponse() {
                // given
                Long memberId = 10L;
                Slice<ChatRoom> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(emptySlice);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 10);

                // then
                assertThat(result.content()).isEmpty();
                assertThat(result.hasNext()).isFalse();
            }

            @Test
            @DisplayName("마지막으로 읽은 메시지가 없으면 전체 미읽음 개수와 내 membership displayName을 사용한다")
            void usesAllUnreadCountAndMyDisplayName_whenLastReadMessageIdIsNull() {
                // given
                Long memberId = 10L;
                Long counterPartId = 20L;
                Long roomId = 41L;

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Member counterPart = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
                ReflectionTestUtils.setField(counterPart, "id", counterPartId);

                ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me, "철수 채팅");
                ReflectionTestUtils.setField(myMembership, "id", 41L);
                ChatRoomMember counterPartMembership = ChatFixture.createJoinedMember(chatRoom, counterPart, "홍길동");
                ReflectionTestUtils.setField(counterPartMembership, "id", 42L);
                chatRoom.addChatRoomMember(myMembership);
                chatRoom.addChatRoomMember(counterPartMembership);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
                given(messageReadStatusRepository.countAllUnreadMessages(roomId, memberId)).willReturn(3);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 10);

                // then
                assertThat(result.hasNext()).isFalse();
                assertThat(result.content()).hasSize(1);
                DirectChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
                assertThat(roomInfo.displayName()).isEqualTo("철수 채팅");
                assertThat(roomInfo.profileImgUrl()).isNull();
                assertThat(roomInfo.isWithdrawn()).isFalse();
                assertThat(roomInfo.unreadCount()).isEqualTo(3);
                assertThat(roomInfo.lastMessage()).isNull();

                verify(messageReadStatusRepository).countAllUnreadMessages(roomId, memberId);
                verify(messageReadStatusRepository, never()).countUnreadMessagesAfter(anyLong(), anyLong(), anyLong());
                verify(fileService, never()).getUrlFromKey(any());
            }

            @Test
            @DisplayName("마지막으로 읽은 메시지가 있으면 이후 미읽음 개수와 프로필 이미지 및 마지막 메시지를 매핑한다")
            void mapsProfileImageLastMessageAndUnreadAfter_whenLastReadMessageExists() {
                // given
                Long memberId = 10L;
                Long counterPartId = 20L;
                Long roomId = 42L;
                LocalDateTime sentAt = LocalDateTime.of(2026, 4, 2, 9, 30);

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Member counterPart = MemberFixture.createMemberWithName("이영희", "영희", Gender.FEMALE, Level.B, 2002L);
                ReflectionTestUtils.setField(counterPart, "id", counterPartId);
                counterPart.updateProfileImg(ProfileImg.builder()
                        .imgKey("member/profile.png")
                        .build());

                ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me, "영희 채팅");
                ReflectionTestUtils.setField(myMembership, "id", 43L);
                myMembership.updateLastReadMessageId(30L);

                ChatRoomMember counterPartMembership = ChatFixture.createJoinedMember(chatRoom, counterPart, "홍길동");
                ReflectionTestUtils.setField(counterPartMembership, "id", 44L);
                chatRoom.addChatRoomMember(myMembership);
                chatRoom.addChatRoomMember(counterPartMembership);

                LastMessageCacheDTO lastMessage = LastMessageCacheDTO.builder()
                        .content("가장 최근 메시지")
                        .timestamp(sentAt)
                        .messageType("TEXT")
                        .build();
                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 5), true);

                given(chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 5)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
                given(messageReadStatusRepository.countUnreadMessagesAfter(roomId, memberId, 30L)).willReturn(2);
                given(chatRoomListCacheService.getLastMessage(roomId)).willReturn(lastMessage);
                given(fileService.getUrlFromKey("member/profile.png")).willReturn("https://cdn.example.com/member/profile.png");

                // when
                DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 5);

                // then
                assertThat(result.hasNext()).isTrue();
                assertThat(result.content()).hasSize(1);
                DirectChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
                assertThat(roomInfo.displayName()).isEqualTo("영희 채팅");
                assertThat(roomInfo.profileImgUrl()).isEqualTo("https://cdn.example.com/member/profile.png");
                assertThat(roomInfo.isWithdrawn()).isFalse();
                assertThat(roomInfo.unreadCount()).isEqualTo(2);
                assertThat(roomInfo.lastMessage()).isNotNull();
                assertThat(roomInfo.lastMessage().content()).isEqualTo("가장 최근 메시지");
                assertThat(roomInfo.lastMessage().timestamp()).isEqualTo(sentAt);
                assertThat(roomInfo.lastMessage().messageType()).isEqualTo("TEXT");

                verify(messageReadStatusRepository).countUnreadMessagesAfter(roomId, memberId, 30L);
                verify(messageReadStatusRepository, never()).countAllUnreadMessages(roomId, memberId);
                verify(chatRoomListCacheService).getLastMessage(roomId);
                verify(fileService).getUrlFromKey("member/profile.png");
            }

            @Test
            @DisplayName("상대방이 탈퇴 상태면 isWithdrawn이 true가 된다")
            void marksCounterPartWithdrawn_whenMemberInactive() {
                // given
                Long memberId = 10L;
                Long roomId = 43L;

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Member withdrawnCounterPart = MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 2002L);
                ReflectionTestUtils.setField(withdrawnCounterPart, "id", 20L);
                withdrawnCounterPart.updateProfileImg(ProfileImg.builder()
                        .imgKey("member/withdrawn-direct.png")
                        .build());

                ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me, "탈퇴한 상대와의 대화");
                ReflectionTestUtils.setField(myMembership, "id", 45L);
                ChatRoomMember counterPartMembership = ChatFixture.createJoinedMember(chatRoom, withdrawnCounterPart, "홍길동");
                ReflectionTestUtils.setField(counterPartMembership, "id", 46L);
                chatRoom.addChatRoomMember(myMembership);
                chatRoom.addChatRoomMember(counterPartMembership);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
                given(messageReadStatusRepository.countAllUnreadMessages(roomId, memberId)).willReturn(0);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 10);

                // then
                assertThat(result.content()).hasSize(1);
                DirectChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.displayName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
                assertThat(roomInfo.profileImgUrl()).isNull();
                assertThat(roomInfo.isWithdrawn()).isTrue();
                verify(fileService, never()).getUrlFromKey("member/withdrawn-direct.png");
            }

            @Test
            @DisplayName("상대방 회원이 hard delete되어 membership member가 null이면 알 수 없는 사용자로 매핑한다")
            void mapsUnknownUser_whenCounterPartMemberIsNull() {
                // given
                Long memberId = 10L;
                Long roomId = 44L;

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me, "삭제된 상대와의 대화");
                ReflectionTestUtils.setField(myMembership, "id", 47L);
                ChatRoomMember deletedCounterPartMembership = ChatRoomMember.createJoined(chatRoom, null, "홍길동");
                ReflectionTestUtils.setField(deletedCounterPartMembership, "id", 48L);
                chatRoom.addChatRoomMember(myMembership);
                chatRoom.addChatRoomMember(deletedCounterPartMembership);

                Slice<ChatRoom> chatRooms = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(chatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
                given(messageReadStatusRepository.countAllUnreadMessages(roomId, memberId)).willReturn(0);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 10);

                // then
                DirectChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.displayName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
                assertThat(roomInfo.profileImgUrl()).isNull();
                assertThat(roomInfo.isWithdrawn()).isTrue();
            }

            @Test
            @DisplayName("채팅방 목록은 repository가 반환한 최신 메시지 순서를 유지한다")
            void preservesRepositoryOrder() {
                // given
                Long memberId = 10L;

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Member newerCounterPart = MemberFixture.createMemberWithName("이영희", "영희", Gender.FEMALE, Level.B, 2002L);
                ReflectionTestUtils.setField(newerCounterPart, "id", 20L);
                Member olderCounterPart = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.C, 3003L);
                ReflectionTestUtils.setField(olderCounterPart, "id", 30L);

                ChatRoom newerRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(newerRoom, "id", 51L);
                ChatRoomMember newerMyMembership = ChatFixture.createJoinedMember(newerRoom, me, "영희 채팅");
                ReflectionTestUtils.setField(newerMyMembership, "id", 51L);
                ChatRoomMember newerCounterPartMembership = ChatFixture.createJoinedMember(newerRoom, newerCounterPart, "홍길동");
                ReflectionTestUtils.setField(newerCounterPartMembership, "id", 52L);
                newerRoom.addChatRoomMember(newerMyMembership);
                newerRoom.addChatRoomMember(newerCounterPartMembership);

                ChatRoom olderRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(olderRoom, "id", 52L);
                ChatRoomMember olderMyMembership = ChatFixture.createJoinedMember(olderRoom, me, "철수 채팅");
                ReflectionTestUtils.setField(olderMyMembership, "id", 53L);
                ChatRoomMember olderCounterPartMembership = ChatFixture.createJoinedMember(olderRoom, olderCounterPart, "홍길동");
                ReflectionTestUtils.setField(olderCounterPartMembership, "id", 54L);
                olderRoom.addChatRoomMember(olderMyMembership);
                olderRoom.addChatRoomMember(olderCounterPartMembership);

                Slice<ChatRoom> orderedChatRooms = new SliceImpl<>(List.of(newerRoom, olderRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, PageRequest.of(0, 10)))
                        .willReturn(orderedChatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(51L, memberId)).willReturn(Optional.of(newerMyMembership));
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(52L, memberId)).willReturn(Optional.of(olderMyMembership));
                given(messageReadStatusRepository.countAllUnreadMessages(51L, memberId)).willReturn(0);
                given(messageReadStatusRepository.countAllUnreadMessages(52L, memberId)).willReturn(0);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.getDirectChatRooms(memberId, 0, 10);

                // then
                assertThat(result.content()).hasSize(2);
                assertThat(result.content().get(0).chatRoomId()).isEqualTo(51L);
                assertThat(result.content().get(0).displayName()).isEqualTo("영희 채팅");
                assertThat(result.content().get(1).chatRoomId()).isEqualTo(52L);
                assertThat(result.content().get(1).displayName()).isEqualTo("철수 채팅");
            }
        }
    }

    @Nested
    @DisplayName("searchDirectChatRoomsByName - 개인 채팅방 이름 검색")
    class SearchDirectChatRoomsByName {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("검색 결과가 없으면 빈 목록과 hasNext false를 반환한다")
            void emptySlice_returnsEmptyResponse() {
                // given
                Long memberId = 10L;
                String name = "영희";
                Slice<ChatRoom> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchDirectChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(emptySlice);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.searchDirectChatRoomsByName(memberId, name, 0, 10);

                // then
                assertThat(result.content()).isEmpty();
                assertThat(result.hasNext()).isFalse();
                verify(chatRoomRepository).searchDirectChatRoomsByName(memberId, name, PageRequest.of(0, 10));
            }

            @Test
            @DisplayName("검색된 개인 채팅방을 현재 사용자 membership displayName 기준으로 매핑한다")
            void mapsMatchedRoomUsingCurrentMembershipDisplayName() {
                // given
                Long memberId = 10L;
                Long roomId = 61L;
                String name = "영희";
                LocalDateTime sentAt = LocalDateTime.of(2026, 4, 5, 8, 45);

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Member withdrawnCounterPart = MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 2002L);
                ReflectionTestUtils.setField(withdrawnCounterPart, "id", 20L);
                withdrawnCounterPart.updateProfileImg(ProfileImg.builder()
                        .imgKey("member/search-profile.png")
                        .build());

                ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me, "영희 채팅");
                ReflectionTestUtils.setField(myMembership, "id", 61L);
                myMembership.updateLastReadMessageId(40L);

                ChatRoomMember counterPartMembership =
                        ChatFixture.createJoinedMember(chatRoom, withdrawnCounterPart, "홍길동");
                ReflectionTestUtils.setField(counterPartMembership, "id", 62L);
                chatRoom.addChatRoomMember(myMembership);
                chatRoom.addChatRoomMember(counterPartMembership);

                LastMessageCacheDTO lastMessage = LastMessageCacheDTO.builder()
                        .content("최근 영희 메시지")
                        .timestamp(sentAt)
                        .messageType("TEXT")
                        .build();
                Slice<ChatRoom> searchResult = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 5), true);

                given(chatRoomRepository.searchDirectChatRoomsByName(memberId, name, PageRequest.of(0, 5)))
                        .willReturn(searchResult);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId))
                        .willReturn(Optional.of(myMembership));
                given(messageReadStatusRepository.countUnreadMessagesAfter(roomId, memberId, 40L))
                        .willReturn(2);
                given(chatRoomListCacheService.getLastMessage(roomId)).willReturn(lastMessage);
                // when
                DirectChatRoomDTO.Response result = chatQueryService.searchDirectChatRoomsByName(memberId, name, 0, 5);

                // then
                assertThat(result.hasNext()).isTrue();
                assertThat(result.content()).hasSize(1);

                DirectChatRoomDTO.ChatRoomInfo roomInfo = result.content().get(0);
                assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
                assertThat(roomInfo.displayName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
                assertThat(roomInfo.profileImgUrl()).isNull();
                assertThat(roomInfo.isWithdrawn()).isTrue();
                assertThat(roomInfo.unreadCount()).isEqualTo(2);
                assertThat(roomInfo.lastMessage()).isNotNull();
                assertThat(roomInfo.lastMessage().content()).isEqualTo("최근 영희 메시지");
                assertThat(roomInfo.lastMessage().timestamp()).isEqualTo(sentAt);
                assertThat(roomInfo.lastMessage().messageType()).isEqualTo("TEXT");

                verify(chatRoomRepository).searchDirectChatRoomsByName(memberId, name, PageRequest.of(0, 5));
                verify(messageReadStatusRepository).countUnreadMessagesAfter(roomId, memberId, 40L);
                verify(fileService, never()).getUrlFromKey("member/search-profile.png");
                verify(chatRoomListCacheService).getLastMessage(roomId);
            }

            @Test
            @DisplayName("검색 결과는 repository가 반환한 최신 메시지 순서를 그대로 유지한다")
            void preservesRepositoryOrder() {
                // given
                Long memberId = 10L;
                String name = "채팅";

                Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
                ReflectionTestUtils.setField(me, "id", memberId);

                Member newerCounterPart = MemberFixture.createMemberWithName("이영희", "영희", Gender.FEMALE, Level.B, 2002L);
                ReflectionTestUtils.setField(newerCounterPart, "id", 20L);
                Member olderCounterPart = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.C, 3003L);
                ReflectionTestUtils.setField(olderCounterPart, "id", 30L);

                ChatRoom newerRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(newerRoom, "id", 71L);
                ChatRoomMember newerMyMembership = ChatFixture.createJoinedMember(newerRoom, me, "영희 채팅");
                ReflectionTestUtils.setField(newerMyMembership, "id", 71L);
                ChatRoomMember newerCounterPartMembership =
                        ChatFixture.createJoinedMember(newerRoom, newerCounterPart, "홍길동");
                ReflectionTestUtils.setField(newerCounterPartMembership, "id", 72L);
                newerRoom.addChatRoomMember(newerMyMembership);
                newerRoom.addChatRoomMember(newerCounterPartMembership);

                ChatRoom olderRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(olderRoom, "id", 72L);
                ChatRoomMember olderMyMembership = ChatFixture.createJoinedMember(olderRoom, me, "철수 채팅");
                ReflectionTestUtils.setField(olderMyMembership, "id", 73L);
                ChatRoomMember olderCounterPartMembership =
                        ChatFixture.createJoinedMember(olderRoom, olderCounterPart, "홍길동");
                ReflectionTestUtils.setField(olderCounterPartMembership, "id", 74L);
                olderRoom.addChatRoomMember(olderMyMembership);
                olderRoom.addChatRoomMember(olderCounterPartMembership);

                Slice<ChatRoom> orderedChatRooms =
                        new SliceImpl<>(List.of(newerRoom, olderRoom), PageRequest.of(1, 2), false);

                given(chatRoomRepository.searchDirectChatRoomsByName(memberId, name, PageRequest.of(1, 2)))
                        .willReturn(orderedChatRooms);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(71L, memberId))
                        .willReturn(Optional.of(newerMyMembership));
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(72L, memberId))
                        .willReturn(Optional.of(olderMyMembership));
                given(messageReadStatusRepository.countAllUnreadMessages(71L, memberId)).willReturn(0);
                given(messageReadStatusRepository.countAllUnreadMessages(72L, memberId)).willReturn(0);

                // when
                DirectChatRoomDTO.Response result = chatQueryService.searchDirectChatRoomsByName(memberId, name, 1, 2);

                // then
                assertThat(result.hasNext()).isFalse();
                assertThat(result.content()).hasSize(2);
                assertThat(result.content().get(0).chatRoomId()).isEqualTo(71L);
                assertThat(result.content().get(0).displayName()).isEqualTo("영희 채팅");
                assertThat(result.content().get(1).chatRoomId()).isEqualTo(72L);
                assertThat(result.content().get(1).displayName()).isEqualTo("철수 채팅");

                verify(chatRoomRepository).searchDirectChatRoomsByName(memberId, name, PageRequest.of(1, 2));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("검색 결과에 현재 사용자 membership이 없으면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
            void throwsAccessDenied_whenMembershipIsMissing() {
                // given
                Long memberId = 10L;
                Long roomId = 81L;
                String name = "영희";

                ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(chatRoom, "id", roomId);

                Slice<ChatRoom> searchResult = new SliceImpl<>(List.of(chatRoom), PageRequest.of(0, 10), false);

                given(chatRoomRepository.searchDirectChatRoomsByName(memberId, name, PageRequest.of(0, 10)))
                        .willReturn(searchResult);
                given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatQueryService.searchDirectChatRoomsByName(memberId, name, 0, 10))
                        .isInstanceOf(ChatException.class)
                        .satisfies(e -> assertThat(((ChatException) e).getCode())
                                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
            }
        }
    }

    @Nested
    @DisplayName("getChatRoomDetail - 초기 채팅방 조회")
    class GetChatRoomDetail {

        @Test
        @DisplayName("모임(PARTY) 채팅방 조회 시 파티 이름이 displayName이 된다")
        void partyChatRoom_success() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("배드민턴 모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMemberWithLastRead(chatRoom, me, 30L);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            List<ChatRoomMember> participants = List.of(myMembership);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of());
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(participants);
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            ChatRoomDetailDTO.ChatRoomInfo roomInfo = result.chatRoomInfo();
            assertThat(roomInfo.chatRoomId()).isEqualTo(roomId);
            assertThat(roomInfo.chatRoomType()).isEqualTo(ChatRoomType.PARTY);
            assertThat(roomInfo.displayName()).isEqualTo("배드민턴 모임");
            assertThat(roomInfo.memberCount()).isEqualTo(1);
            assertThat(roomInfo.lastReadMessageId()).isEqualTo(30L);
            assertThat(roomInfo.isCounterPartWithdrawn()).isFalse();
            assertThat(result.messages()).isEmpty();
            assertThat(result.participants()).hasSize(1);
            assertThat(result.participants().get(0).memberName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("개인(DIRECT) 채팅방 조회 시 상대방 이름이 displayName이 된다")
        void directChatRoom_success() {
            // given
            Long roomId = 2L;
            Long memberId = 10L;
            Long counterPartId = 20L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Member counterPart = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
            ReflectionTestUtils.setField(counterPart, "id", counterPartId);

            ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatRoomMember counterPartMembership = ChatFixture.createJoinedMember(chatRoom, counterPart);
            ReflectionTestUtils.setField(counterPartMembership, "id", 2L);

            List<ChatRoomMember> participants = List.of(myMembership, counterPartMembership);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatRoomMemberRepository.findCounterPartWithMember(roomId, memberId)).willReturn(Optional.of(counterPartMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(2);
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of());
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(participants);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            ChatRoomDetailDTO.ChatRoomInfo roomInfo = result.chatRoomInfo();
            assertThat(roomInfo.chatRoomType()).isEqualTo(ChatRoomType.DIRECT);
            assertThat(roomInfo.displayName()).isEqualTo("김철수");
            assertThat(roomInfo.memberCount()).isEqualTo(2);
            assertThat(roomInfo.isCounterPartWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("개인 채팅방에서 상대방이 탈퇴한 경우 isCounterPartWithdrawn이 true이다")
        void directChatRoom_counterPartWithdrawn() {
            // given
            Long roomId = 3L;
            Long memberId = 10L;
            Long counterPartId = 20L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Member withdrawnCounterPart = MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 2002L);
            ReflectionTestUtils.setField(withdrawnCounterPart, "id", counterPartId);
            withdrawnCounterPart.updateProfileImg(ProfileImg.builder()
                    .imgKey("member/withdrawn-detail.png")
                    .build());

            ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatRoomMember withdrawnMembership = ChatFixture.createJoinedMember(chatRoom, withdrawnCounterPart);
            ReflectionTestUtils.setField(withdrawnMembership, "id", 2L);

            List<ChatRoomMember> participants = List.of(myMembership, withdrawnMembership);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatRoomMemberRepository.findCounterPartWithMember(roomId, memberId)).willReturn(Optional.of(withdrawnMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(2);
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of());
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(participants);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.chatRoomInfo().displayName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(result.chatRoomInfo().profileImageUrl()).isNull();
            assertThat(result.chatRoomInfo().isCounterPartWithdrawn()).isTrue();
            assertThat(result.participants().get(1).memberName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(result.participants().get(1).profileImgUrl()).isNull();
            verify(fileService, never()).getUrlFromKey("member/withdrawn-detail.png");
        }

        @Test
        @DisplayName("개인 채팅방에서 상대방 회원이 hard delete된 경우 알 수 없는 사용자로 조회된다")
        void directChatRoom_counterPartMemberNull() {
            // given
            Long roomId = 4L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatRoomMember deletedCounterPartMembership = ChatRoomMember.createJoined(chatRoom, null, "홍길동");
            ReflectionTestUtils.setField(deletedCounterPartMembership, "id", 2L);

            List<ChatRoomMember> participants = List.of(myMembership, deletedCounterPartMembership);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatRoomMemberRepository.findCounterPartWithMember(roomId, memberId)).willReturn(Optional.of(deletedCounterPartMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(2);
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of());
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(participants);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.chatRoomInfo().displayName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(result.chatRoomInfo().profileImageUrl()).isNull();
            assertThat(result.chatRoomInfo().isCounterPartWithdrawn()).isTrue();
            assertThat(result.participants().get(1).memberId()).isNull();
            assertThat(result.participants().get(1).memberName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(result.participants().get(1).profileImgUrl()).isNull();
        }

        @Test
        @DisplayName("메시지가 DB에서 최신순으로 반환되면, 최종 응답에서는 오래된 순으로 뒤집혀야 한다")
        void messages_areReversedToChronologicalOrder() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            // DB는 최신순(id: 3→2→1)으로 반환
            ChatMessage msg1 = ChatFixture.createTextMessage(chatRoom, me, "첫 번째 메시지");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            ChatMessage msg2 = ChatFixture.createTextMessage(chatRoom, me, "두 번째 메시지");
            ReflectionTestUtils.setField(msg2, "id", 2L);
            ChatMessage msg3 = ChatFixture.createTextMessage(chatRoom, me, "세 번째 메시지");
            ReflectionTestUtils.setField(msg3, "id", 3L);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(msg3, msg2, msg1));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then: 응답 메시지는 오래된 순(1→2→3)
            List<ChatRoomDetailDTO.MessageInfo> messages = result.messages();
            assertThat(messages).hasSize(3);
            assertThat(messages.get(0).messageId()).isEqualTo(1L);
            assertThat(messages.get(1).messageId()).isEqualTo(2L);
            assertThat(messages.get(2).messageId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("메시지 발신자가 나(memberId)이면 isMyMessage가 true이다")
        void message_isMyMessage_true_whenSenderIsMe() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatMessage myMessage = ChatFixture.createTextMessage(chatRoom, me, "내 메시지");
            ReflectionTestUtils.setField(myMessage, "id", 1L);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(myMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.messages().get(0).isMyMessage()).isTrue();
            assertThat(result.messages().get(0).content()).isEqualTo("내 메시지");
            assertThat(result.messages().get(0).senderName()).isEqualTo("홍길동");
            assertThat(result.messages().get(0).images()).isEmpty();
        }

        @Test
        @DisplayName("메시지 발신자가 상대방이면 isMyMessage가 false이다")
        void message_isMyMessage_false_whenSenderIsOther() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long otherId = 20L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Member other = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
            ReflectionTestUtils.setField(other, "id", otherId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatRoomMember otherMembership = ChatFixture.createJoinedMember(chatRoom, other);
            ReflectionTestUtils.setField(otherMembership, "id", 2L);

            ChatMessage otherMessage = ChatFixture.createTextMessage(chatRoom, other, "상대방 메시지");
            ReflectionTestUtils.setField(otherMessage, "id", 1L);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(otherMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership, otherMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(2);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.messages().get(0).isMyMessage()).isFalse();
            assertThat(result.messages().get(0).isSenderWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("탈퇴한 사용자가 보낸 메시지는 isSenderWithdrawn이 true이다")
        void message_isSenderWithdrawn_true() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long withdrawnId = 30L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Member withdrawn = MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L);
            ReflectionTestUtils.setField(withdrawn, "id", withdrawnId);
            withdrawn.updateProfileImg(ProfileImg.builder()
                    .imgKey("member/withdrawn-message.png")
                    .build());

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatRoomMember withdrawnMembership = ChatFixture.createJoinedMember(chatRoom, withdrawn);
            ReflectionTestUtils.setField(withdrawnMembership, "id", 2L);

            ChatMessage withdrawnMessage = ChatFixture.createTextMessage(chatRoom, withdrawn, "탈퇴자 메시지");
            ReflectionTestUtils.setField(withdrawnMessage, "id", 1L);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(withdrawnMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership, withdrawnMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(2);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            ChatRoomDetailDTO.MessageInfo messageInfo = result.messages().get(0);
            assertThat(messageInfo.isSenderWithdrawn()).isTrue();
            assertThat(messageInfo.senderName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(messageInfo.senderProfileImageUrl()).isNull();
            verify(fileService, never()).getUrlFromKey("member/withdrawn-message.png");
        }

        @Test
        @DisplayName("sender가 null인 일반 메시지는 알 수 없는 사용자로 조회된다")
        void message_nullSender_isMappedToUnknownUser() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatMessage deletedSenderMessage = ChatMessage.create(chatRoom, null, "삭제된 사용자 메시지", MessageType.TEXT);
            ReflectionTestUtils.setField(deletedSenderMessage, "id", 1L);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(deletedSenderMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            ChatRoomDetailDTO.MessageInfo messageInfo = result.messages().get(0);
            assertThat(messageInfo.senderId()).isNull();
            assertThat(messageInfo.senderName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(messageInfo.senderProfileImageUrl()).isNull();
            assertThat(messageInfo.isSenderWithdrawn()).isTrue();
            assertThat(messageInfo.isMyMessage()).isFalse();
        }

        @Test
        @DisplayName("시스템 메시지는 sender 없이도 조회되고 시스템 기본값으로 매핑된다")
        void systemMessage_isMappedWithSystemDefaults() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatMessage systemMessage = ChatFixture.createSystemMessage(chatRoom, "공지 메시지");
            ReflectionTestUtils.setField(systemMessage, "id", 1L);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(systemMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.messages()).hasSize(1);
            ChatRoomDetailDTO.MessageInfo message = result.messages().get(0);
            assertThat(message.messageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(message.senderId()).isNull();
            assertThat(message.senderName()).isEqualTo("시스템");
            assertThat(message.senderProfileImageUrl()).isNull();
            assertThat(message.isMyMessage()).isFalse();
            assertThat(message.isSenderWithdrawn()).isFalse();
            assertThat(message.content()).isEqualTo("공지 메시지");
        }

        @Test
        @DisplayName("이미지 메시지 조회 시 images 필드에 파일 정보가 포함된다")
        void imageMessage_containsFileInfo() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatRoomMember myMembership = ChatFixture.createJoinedMember(chatRoom, me);
            ReflectionTestUtils.setField(myMembership, "id", 1L);

            ChatMessage imageMessage = ChatFixture.createImageMessage(chatRoom, me, List.of());
            ReflectionTestUtils.setField(imageMessage, "id", 1L);

            ChatMessageFile file1 = ChatFixture.createChatMessageFile(imageMessage, "chat/img1.png", 1, "photo1.png");
            ReflectionTestUtils.setField(file1, "id", 100L);
            ChatMessageFile file2 = ChatFixture.createChatMessageFile(imageMessage, "chat/img2.png", 2, "photo2.png");
            ReflectionTestUtils.setField(file2, "id", 101L);
            imageMessage.getChatMessageFiles().addAll(List.of(file1, file2));

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.of(myMembership));
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of(imageMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);
            given(fileService.getUrlFromKey("chat/img1.png")).willReturn("https://storage.example.com/chat/img1.png");
            given(fileService.getUrlFromKey("chat/img2.png")).willReturn("https://storage.example.com/chat/img2.png");

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            ChatRoomDetailDTO.MessageInfo message = result.messages().get(0);
            assertThat(message.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(message.images()).hasSize(2);
            assertThat(message.images().get(0).imageUrl()).isEqualTo("https://storage.example.com/chat/img1.png");
            assertThat(message.images().get(0).imgOrder()).isEqualTo(1);
            assertThat(message.images().get(0).originalFileName()).isEqualTo("photo1.png");
            assertThat(message.images().get(0).isEmoji()).isFalse();
            assertThat(message.images().get(1).imageUrl()).isEqualTo("https://storage.example.com/chat/img2.png");
            assertThat(message.images().get(1).imgOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방 조회 시 ChatException(CHAT_ROOM_NOT_FOUND)을 던진다")
        void fail_chatRoomNotFound() {
            // given
            given(chatRoomRepository.findChatRoomWithPartyById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatQueryService.getChatRoomDetail(999L, 10L))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자가 조회하면 ChatException(CHAT_ROOM_ACCESS_DENIED)을 던진다")
        void fail_notChatRoomMember() {
            // given
            Long roomId = 1L;
            Long outsiderId = 99L;

            Party party = PartyFixture.createParty("모임", 1L, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            given(chatRoomRepository.findChatRoomWithPartyById(roomId)).willReturn(Optional.of(chatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, outsiderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatQueryService.getChatRoomDetail(roomId, outsiderId))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

            verify(chatRoomRepository).findChatRoomWithPartyById(roomId);
        }
    }

    @Nested
    @DisplayName("getChatMessages - 과거 메시지 조회")
    class GetChatMessages {

        @Test
        @DisplayName("cursor 이전 메시지가 size 이하이면 hasNext가 false이고 nextCursor가 null이다")
        void noMoreMessages_hasNextFalse() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;
            int size = 3;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            // DB는 최신순(id: 3→2→1)으로 반환, size+1=4개 요청했지만 3개만 존재
            ChatMessage msg1 = ChatFixture.createTextMessage(chatRoom, me, "첫 번째 메시지");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            ChatMessage msg2 = ChatFixture.createTextMessage(chatRoom, me, "두 번째 메시지");
            ReflectionTestUtils.setField(msg2, "id", 2L);
            ChatMessage msg3 = ChatFixture.createTextMessage(chatRoom, me, "세 번째 메시지");
            ReflectionTestUtils.setField(msg3, "id", 3L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(msg3, msg2, msg1));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, size);

            // then
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.messages()).hasSize(3);
        }

        @Test
        @DisplayName("cursor 이전 메시지가 size보다 많으면 hasNext가 true이고 nextCursor가 설정된다")
        void moreMessagesExist_hasNextTrue() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;
            int size = 2;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            // DB는 최신순(3→2→1)으로 size+1=3개 반환 → hasNext=true
            ChatMessage msg1 = ChatFixture.createTextMessage(chatRoom, me, "첫 번째 메시지");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            ChatMessage msg2 = ChatFixture.createTextMessage(chatRoom, me, "두 번째 메시지");
            ReflectionTestUtils.setField(msg2, "id", 2L);
            ChatMessage msg3 = ChatFixture.createTextMessage(chatRoom, me, "세 번째 메시지");
            ReflectionTestUtils.setField(msg3, "id", 3L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(msg3, msg2, msg1));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, size);

            // then
            assertThat(result.hasNext()).isTrue();
            // size개 자른 후 가장 오래된 메시지(subList[0])의 id
            assertThat(result.nextCursor()).isEqualTo(2L);
            assertThat(result.messages()).hasSize(2);
        }

        @Test
        @DisplayName("반환된 메시지는 오래된 순(오름차순)으로 정렬된다")
        void messages_areInChronologicalOrder() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;
            int size = 3;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            // DB에서 최신순(3→2→1) 반환
            ChatMessage msg1 = ChatFixture.createTextMessage(chatRoom, me, "첫 번째");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            ChatMessage msg2 = ChatFixture.createTextMessage(chatRoom, me, "두 번째");
            ReflectionTestUtils.setField(msg2, "id", 2L);
            ChatMessage msg3 = ChatFixture.createTextMessage(chatRoom, me, "세 번째");
            ReflectionTestUtils.setField(msg3, "id", 3L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(msg3, msg2, msg1));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, size);

            // then: 응답은 오래된 순(1→2→3)
            List<ChatMessageDTO.MessageInfo> messages = result.messages();
            assertThat(messages).hasSize(3);
            assertThat(messages.get(0).messageId()).isEqualTo(1L);
            assertThat(messages.get(1).messageId()).isEqualTo(2L);
            assertThat(messages.get(2).messageId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("내가 보낸 메시지는 isMyMessage가 true이다")
        void myMessage_isMyMessageTrue() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatMessage myMessage = ChatFixture.createTextMessage(chatRoom, me, "내 메시지");
            ReflectionTestUtils.setField(myMessage, "id", 1L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(myMessage));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, 10);

            // then
            assertThat(result.messages().get(0).isMyMessage()).isTrue();
            assertThat(result.messages().get(0).images()).isEmpty();
        }

        @Test
        @DisplayName("상대방이 보낸 메시지는 isMyMessage가 false이다")
        void otherMessage_isMyMessageFalse() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long otherId = 20L;
            Long cursor = 100L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Member other = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
            ReflectionTestUtils.setField(other, "id", otherId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatMessage otherMessage = ChatFixture.createTextMessage(chatRoom, other, "상대방 메시지");
            ReflectionTestUtils.setField(otherMessage, "id", 1L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(otherMessage));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, 10);

            // then
            assertThat(result.messages().get(0).isMyMessage()).isFalse();
            assertThat(result.messages().get(0).senderName()).isEqualTo("김철수");
        }

        @Test
        @DisplayName("탈퇴한 사용자가 보낸 메시지는 isSenderWithdrawn이 true이다")
        void withdrawnSenderMessage_isSenderWithdrawnTrue() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long withdrawnId = 30L;
            Long cursor = 100L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Member withdrawn = MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L);
            ReflectionTestUtils.setField(withdrawn, "id", withdrawnId);
            withdrawn.updateProfileImg(ProfileImg.builder()
                    .imgKey("member/withdrawn-previous.png")
                    .build());

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatMessage withdrawnMessage = ChatFixture.createTextMessage(chatRoom, withdrawn, "탈퇴자 메시지");
            ReflectionTestUtils.setField(withdrawnMessage, "id", 1L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(withdrawnMessage));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, 10);

            // then
            ChatMessageDTO.MessageInfo messageInfo = result.messages().get(0);
            assertThat(messageInfo.isSenderWithdrawn()).isTrue();
            assertThat(messageInfo.senderName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(messageInfo.senderProfileImageUrl()).isNull();
            verify(fileService, never()).getUrlFromKey("member/withdrawn-previous.png");
        }

        @Test
        @DisplayName("sender가 null인 일반 과거 메시지는 알 수 없는 사용자로 조회된다")
        void nullSenderPreviousMessage_isMappedToUnknownUser() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatMessage deletedSenderMessage = ChatMessage.create(chatRoom, null, "삭제된 사용자 메시지", MessageType.TEXT);
            ReflectionTestUtils.setField(deletedSenderMessage, "id", 1L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(deletedSenderMessage));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, 10);

            // then
            ChatMessageDTO.MessageInfo messageInfo = result.messages().get(0);
            assertThat(messageInfo.senderId()).isNull();
            assertThat(messageInfo.senderName()).isEqualTo(ChatConverter.UNKNOWN_USER_NAME);
            assertThat(messageInfo.senderProfileImageUrl()).isNull();
            assertThat(messageInfo.isSenderWithdrawn()).isTrue();
            assertThat(messageInfo.isMyMessage()).isFalse();
        }

        @Test
        @DisplayName("시스템 메시지는 sender 없이도 과거 메시지 응답에 포함된다")
        void systemMessage_includedInPreviousMessages() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatMessage systemMessage = ChatFixture.createSystemMessage(chatRoom, "홍길동님이 모임에 참여하셨습니다.");
            ReflectionTestUtils.setField(systemMessage, "id", 1L);

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(systemMessage));

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, 10);

            // then
            assertThat(result.messages()).hasSize(1);
            ChatMessageDTO.MessageInfo message = result.messages().get(0);
            assertThat(message.messageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(message.senderId()).isNull();
            assertThat(message.senderName()).isEqualTo("시스템");
            assertThat(message.senderProfileImageUrl()).isNull();
            assertThat(message.isMyMessage()).isFalse();
            assertThat(message.isSenderWithdrawn()).isFalse();
            assertThat(message.content()).isEqualTo("홍길동님이 모임에 참여하셨습니다.");
        }

        @Test
        @DisplayName("이미지 메시지 조회 시 images 필드에 파일 정보가 포함된다")
        void imageMessage_containsFileInfo() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            Long cursor = 100L;

            Member me = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(me, "id", memberId);

            Party party = PartyFixture.createParty("모임", memberId, PartyFixture.createPartyAddr("서울", "강남구"));
            ReflectionTestUtils.setField(party, "id", 100L);

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);

            ChatMessage imageMessage = ChatFixture.createImageMessage(chatRoom, me, List.of());
            ReflectionTestUtils.setField(imageMessage, "id", 1L);

            ChatMessageFile file1 = ChatFixture.createChatMessageFile(imageMessage, "chat/img1.png", 1, "photo1.png");
            ReflectionTestUtils.setField(file1, "id", 100L);
            ChatMessageFile file2 = ChatFixture.createChatMessageFile(imageMessage, "chat/img2.png", 2, "photo2.png");
            ReflectionTestUtils.setField(file2, "id", 101L);
            imageMessage.getChatMessageFiles().addAll(List.of(file1, file2));

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)).willReturn(true);
            given(chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomId), eq(cursor), any()))
                    .willReturn(List.of(imageMessage));
            given(fileService.getUrlFromKey("chat/img1.png")).willReturn("https://storage.example.com/chat/img1.png");
            given(fileService.getUrlFromKey("chat/img2.png")).willReturn("https://storage.example.com/chat/img2.png");

            // when
            ChatMessageDTO.Response result = chatQueryService.getChatMessages(roomId, memberId, cursor, 10);

            // then
            ChatMessageDTO.MessageInfo message = result.messages().get(0);
            assertThat(message.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(message.images()).hasSize(2);
            assertThat(message.images().get(0).imageUrl()).isEqualTo("https://storage.example.com/chat/img1.png");
            assertThat(message.images().get(0).imgOrder()).isEqualTo(1);
            assertThat(message.images().get(0).originalFileName()).isEqualTo("photo1.png");
            assertThat(message.images().get(0).isEmoji()).isFalse();
            assertThat(message.images().get(1).imageUrl()).isEqualTo("https://storage.example.com/chat/img2.png");
            assertThat(message.images().get(1).imgOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자가 메시지를 조회하면 ChatException(CHAT_ROOM_ACCESS_DENIED)을 던진다")
        void fail_notChatRoomMember() {
            // given
            Long roomId = 1L;
            Long outsiderId = 99L;
            Long cursor = 100L;

            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, outsiderId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatQueryService.getChatMessages(roomId, outsiderId, cursor, 10))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
        }
    }
}
