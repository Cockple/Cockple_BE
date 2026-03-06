package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    private ChatQueryServiceImpl chatQueryService;

    @BeforeEach
    void setUp() {
        chatConverter = new ChatConverter();
        chatProcessor = new ChatProcessor(fileService, chatConverter);
        chatQueryService = new ChatQueryServiceImpl(
                chatRoomRepository,
                chatRoomMemberRepository,
                chatMessageRepository,
                partyRepository,
                memberPartyRepository,
                messageReadStatusRepository,
                chatConverter,
                fileService,
                chatProcessor,
                chatRoomListCacheService
        );
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of());
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of());
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of());
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(participants);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.chatRoomInfo().isCounterPartWithdrawn()).isTrue();
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of(msg3, msg2, msg1));
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of(myMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.messages().get(0).isMyMessage()).isTrue();
            assertThat(result.messages().get(0).content()).isEqualTo("내 메시지");
            assertThat(result.messages().get(0).senderName()).isEqualTo("홍길동");
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of(otherMessage));
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
            given(chatMessageRepository.findRecentMessagesWithImages(eq(roomId), any())).willReturn(List.of(withdrawnMessage));
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(List.of(myMembership, withdrawnMembership));
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(2);

            // when
            ChatRoomDetailDTO.Response result = chatQueryService.getChatRoomDetail(roomId, memberId);

            // then
            assertThat(result.messages().get(0).isSenderWithdrawn()).isTrue();
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
            assertThat(result.messages().get(0).isSenderWithdrawn()).isTrue();
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