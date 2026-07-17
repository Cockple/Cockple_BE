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
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.LastMessageCacheDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatRoomUnreadCountDTO;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DirectChatRoomQueryServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomListCacheService chatRoomListCacheService;
    @Mock private FileService fileService;

    private ChatConverter chatConverter;
    private ChatUnreadQueryService chatUnreadQueryService;
    private ImageUrlResolver imageUrlResolver;
    private DirectChatRoomQueryService directChatRoomQueryService;

    @BeforeEach
    void setUp() {
        chatConverter = new ChatConverter();
        chatUnreadQueryService = new ChatUnreadQueryService(messageReadStatusRepository);
        imageUrlResolver = new ImageUrlResolver(fileService);
        directChatRoomQueryService = new DirectChatRoomQueryService(
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
                DirectChatRoomDTO.Response result = directChatRoomQueryService.getDirectChatRooms(memberId, 0, 10);

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
                given(messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, List.of(roomId)))
                        .willReturn(List.of(new ChatRoomUnreadCountDTO(roomId, 3L)));

                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.getDirectChatRooms(memberId, 0, 10);

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

                verify(messageReadStatusRepository).countUnreadMessagesByChatRooms(memberId, List.of(roomId));
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
                given(messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, List.of(roomId)))
                        .willReturn(List.of(new ChatRoomUnreadCountDTO(roomId, 2L)));
                given(chatRoomListCacheService.getLastMessage(roomId)).willReturn(lastMessage);
                given(fileService.getUrlFromKey("member/profile.png")).willReturn("https://cdn.example.com/member/profile.png");

                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.getDirectChatRooms(memberId, 0, 5);

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

                verify(messageReadStatusRepository).countUnreadMessagesByChatRooms(memberId, List.of(roomId));
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

                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.getDirectChatRooms(memberId, 0, 10);

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

                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.getDirectChatRooms(memberId, 0, 10);

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

                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.getDirectChatRooms(memberId, 0, 10);

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
                DirectChatRoomDTO.Response result = directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, 0, 10);

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
                given(messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, List.of(roomId)))
                        .willReturn(List.of(new ChatRoomUnreadCountDTO(roomId, 2L)));
                given(chatRoomListCacheService.getLastMessage(roomId)).willReturn(lastMessage);
                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, 0, 5);

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
                verify(messageReadStatusRepository).countUnreadMessagesByChatRooms(memberId, List.of(roomId));
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

                // when
                DirectChatRoomDTO.Response result = directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, 1, 2);

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
                assertThatThrownBy(() -> directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, 0, 10))
                        .isInstanceOf(ChatException.class)
                        .satisfies(e -> assertThat(((ChatException) e).getCode())
                                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
            }
        }
    }
}
