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
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.service.support.assembler.ChatMessageViewAssembler;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageHistoryQueryServiceTest {

    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private FileService fileService;

    private ChatMessageHistoryQueryService chatMessageHistoryQueryService;

    @BeforeEach
    void setUp() {
        ChatConverter chatConverter = new ChatConverter();
        ChatMessageViewAssembler chatMessageViewAssembler = new ChatMessageViewAssembler(fileService, chatConverter);
        chatMessageHistoryQueryService = new ChatMessageHistoryQueryService(
                chatRoomMemberRepository,
                chatMessageRepository,
                chatMessageViewAssembler,
                chatConverter
        );
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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, 10);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, 10);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, 10);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, 10);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, 10);

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
            ChatMessageDTO.Response result = chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, 10);

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
            assertThatThrownBy(() -> chatMessageHistoryQueryService.getChatMessages(roomId, outsiderId, cursor, 10))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
        }
    }
}
