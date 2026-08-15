package umc.cockple.demo.domain.chat.service.query;

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
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.service.support.assembler.ChatMessageViewAssembler;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.party.domain.Party;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomDetailQueryServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private FileService fileService;

    private ChatRoomDetailQueryService chatRoomDetailQueryService;

    @BeforeEach
    void setUp() {
        ChatConverter chatConverter = new ChatConverter();
        ImageUrlResolver imageUrlResolver = new ImageUrlResolver(fileService);
        ChatMessageViewAssembler chatMessageViewAssembler = new ChatMessageViewAssembler(imageUrlResolver, chatConverter);
        chatRoomDetailQueryService = new ChatRoomDetailQueryService(
                chatRoomRepository,
                chatRoomMemberRepository,
                chatMessageRepository,
                chatConverter,
                imageUrlResolver,
                chatMessageViewAssembler
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
            given(chatMessageRepository.findRecentMessagesWithFiles(eq(roomId), any())).willReturn(List.of());
            given(chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId)).willReturn(participants);
            given(chatRoomMemberRepository.countByChatRoomId(roomId)).willReturn(1);

            // when
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            ChatRoomDetailDTO.Response result = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);

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
            assertThatThrownBy(() -> chatRoomDetailQueryService.getChatRoomDetail(999L, 10L))
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
            assertThatThrownBy(() -> chatRoomDetailQueryService.getChatRoomDetail(roomId, outsiderId))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

            verify(chatRoomRepository).findChatRoomWithPartyById(roomId);
        }
    }
}
