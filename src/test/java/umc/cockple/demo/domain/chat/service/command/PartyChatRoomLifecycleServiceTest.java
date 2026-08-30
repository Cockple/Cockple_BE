package umc.cockple.demo.domain.chat.service.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.events.ChatRoomRedisCleanupEvent;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomReader;
import umc.cockple.demo.domain.file.service.ObjectStorageDeleteOutboxService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyChatRoomLifecycleService 단위 테스트")
class PartyChatRoomLifecycleServiceTest {

    @InjectMocks
    private PartyChatRoomLifecycleService partyChatRoomLifecycleService;

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatFileRepository chatFileRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private MessageReadStatusRepository messageReadStatusRepository;
    @Mock
    private ObjectStorageDeleteOutboxService objectStorageDeleteOutboxService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ChatRoomReader chatRoomReader;

    @Nested
    @DisplayName("createPartyChatRoom")
    class CreatePartyChatRoom {

        @Test
        @DisplayName("성공 - PARTY 채팅방을 생성하고 owner를 JOINED 멤버로 포함해 저장한다")
        void success_createPartyChatRoom() {
            Long partyId = 1L;
            Member owner = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(owner, "id", 10L);
            var party = PartyFixture.createParty("테스트 모임", owner.getId(), PartyFixture.createPartyAddr("서울", "강남"));
            ReflectionTestUtils.setField(party, "id", partyId);
            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

            partyChatRoomLifecycleService.createPartyChatRoom(party, owner);

            ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(chatRoomCaptor.capture());
            ChatRoom savedChatRoom = chatRoomCaptor.getValue();
            assertThat(savedChatRoom.getType()).isEqualTo(ChatRoomType.PARTY);
            assertThat(savedChatRoom.getParty()).isSameAs(party);
            assertThat(savedChatRoom.getChatRoomMembers()).hasSize(1);

            ChatRoomMember savedMember = savedChatRoom.getChatRoomMembers().get(0);
            assertThat(savedMember.getChatRoom()).isSameAs(savedChatRoom);
            assertThat(savedMember.getMember()).isSameAs(owner);
            assertThat(savedMember.getStatus()).isEqualTo(ChatRoomMemberStatus.JOINED);
        }
    }

    @Nested
    @DisplayName("joinPartyChatRoom")
    class JoinPartyChatRoom {

        @Test
        @DisplayName("성공 - 기존 PARTY 채팅방에 멤버를 JOINED 상태로 저장한다")
        void success_joinPartyChatRoom() {
            Long partyId = 1L;
            Long chatRoomId = 2L;
            Member member = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 1002L);
            ReflectionTestUtils.setField(member, "id", 20L);
            var party = PartyFixture.createParty("테스트 모임", 10L, PartyFixture.createPartyAddr("서울", "강남"));
            ReflectionTestUtils.setField(party, "id", partyId);
            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
            given(chatRoomReader.readByPartyId(partyId)).willReturn(chatRoom);

            partyChatRoomLifecycleService.joinPartyChatRoom(partyId, member);

            ArgumentCaptor<ChatRoomMember> chatRoomMemberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
            verify(chatRoomMemberRepository).save(chatRoomMemberCaptor.capture());
            ChatRoomMember savedMember = chatRoomMemberCaptor.getValue();
            assertThat(savedMember.getChatRoom()).isSameAs(chatRoom);
            assertThat(savedMember.getMember()).isSameAs(member);
            assertThat(savedMember.getStatus()).isEqualTo(ChatRoomMemberStatus.JOINED);
        }

        @Test
        @DisplayName("실패 - PARTY 채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외를 던지고 저장하지 않는다")
        void fail_joinPartyChatRoom_whenRoomMissing() {
            Long partyId = 1L;
            Member member = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 1002L);
            ReflectionTestUtils.setField(member, "id", 20L);
            given(chatRoomReader.readByPartyId(partyId))
                    .willThrow(new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

            assertThatThrownBy(() -> partyChatRoomLifecycleService.joinPartyChatRoom(partyId, member))
                    .isInstanceOfSatisfying(ChatException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
            verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
        }
    }

    @Nested
    @DisplayName("leavePartyChatRoom")
    class LeavePartyChatRoom {

        @Test
        @DisplayName("성공 - 참여 중인 멤버가 있으면 해당 멤버십을 삭제한다")
        void success_leavePartyChatRoom() {
            Long partyId = 1L;
            Long chatRoomId = 2L;
            Long memberId = 20L;
            var party = PartyFixture.createParty("테스트 모임", 10L, PartyFixture.createPartyAddr("서울", "강남"));
            ReflectionTestUtils.setField(party, "id", partyId);
            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
            Member member = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 1002L);
            ReflectionTestUtils.setField(member, "id", memberId);
            ChatRoomMember chatRoomMember = ChatRoomMember.create(chatRoom, member);
            given(chatRoomReader.readByPartyId(partyId)).willReturn(chatRoom);
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId))
                    .willReturn(Optional.of(chatRoomMember));

            partyChatRoomLifecycleService.leavePartyChatRoom(partyId, memberId);

            verify(chatRoomMemberRepository).delete(chatRoomMember);
        }

        @Test
        @DisplayName("성공 - 참여 중인 멤버가 없으면 삭제하지 않고 종료한다")
        void success_leavePartyChatRoom_whenMembershipMissing() {
            Long partyId = 1L;
            Long chatRoomId = 2L;
            Long memberId = 20L;
            var party = PartyFixture.createParty("테스트 모임", 10L, PartyFixture.createPartyAddr("서울", "강남"));
            ReflectionTestUtils.setField(party, "id", partyId);
            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
            given(chatRoomReader.readByPartyId(partyId)).willReturn(chatRoom);
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId))
                    .willReturn(Optional.empty());

            partyChatRoomLifecycleService.leavePartyChatRoom(partyId, memberId);

            verify(chatRoomMemberRepository, never()).delete(any(ChatRoomMember.class));
        }

        @Test
        @DisplayName("실패 - PARTY 채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외를 던지고 멤버십을 조회하지 않는다")
        void fail_leavePartyChatRoom_whenRoomMissing() {
            Long partyId = 1L;
            Long memberId = 20L;
            given(chatRoomReader.readByPartyId(partyId))
                    .willThrow(new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

            assertThatThrownBy(() -> partyChatRoomLifecycleService.leavePartyChatRoom(partyId, memberId))
                    .isInstanceOfSatisfying(ChatException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
            verify(chatRoomMemberRepository, never()).findByChatRoomIdAndMemberId(anyLong(), anyLong());
            verify(chatRoomMemberRepository, never()).delete(any(ChatRoomMember.class));
        }
    }

    @Nested
    @DisplayName("deletePartyChatRoom")
    class DeletePartyChatRoom {

        @Test
        @DisplayName("성공 - 채팅방이 있으면 읽음 상태, 파일, 메시지, 멤버, 방을 삭제하고 Redis 정리 이벤트를 발행한다")
        void success_deletePartyChatRoom() {
            Long partyId = 1L;
            Long chatRoomId = 2L;

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                    PartyFixture.createParty("테스트 모임", 10L, PartyFixture.createPartyAddr("서울", "강남"))
            );
            ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
            List<String> objectKeys = List.of("chat/a.jpg", "chat/b.jpg");

            given(chatRoomReader.findByPartyId(partyId)).willReturn(Optional.of(chatRoom));
            given(chatFileRepository.findObjectKeysByChatRoomId(chatRoomId)).willReturn(objectKeys);

            partyChatRoomLifecycleService.deletePartyChatRoom(partyId);

            var inOrder = inOrder(
                    chatFileRepository,
                    objectStorageDeleteOutboxService,
                    messageReadStatusRepository,
                    chatMessageRepository,
                    chatRoomMemberRepository,
                    chatRoomRepository,
                    applicationEventPublisher
            );
            inOrder.verify(chatFileRepository).findObjectKeysByChatRoomId(chatRoomId);
            inOrder.verify(objectStorageDeleteOutboxService).enqueuePartyChatFiles(chatRoomId, objectKeys);
            inOrder.verify(messageReadStatusRepository).deleteByChatRoomId(chatRoomId);
            inOrder.verify(chatFileRepository).deleteByChatRoomId(chatRoomId);
            inOrder.verify(chatMessageRepository).deleteByChatRoomId(chatRoomId);
            inOrder.verify(chatRoomMemberRepository).deleteByChatRoomId(chatRoomId);
            inOrder.verify(chatRoomRepository).deleteRoomById(chatRoomId);
            inOrder.verify(applicationEventPublisher).publishEvent(new ChatRoomRedisCleanupEvent(chatRoomId));
        }

        @Test
        @DisplayName("성공 - 채팅방이 없으면 아무것도 삭제하지 않고 종료한다")
        void success_deletePartyChatRoom_whenRoomMissing() {
            Long partyId = 1L;
            given(chatRoomReader.findByPartyId(partyId)).willReturn(Optional.empty());

            partyChatRoomLifecycleService.deletePartyChatRoom(partyId);

            verify(messageReadStatusRepository, never()).deleteByChatRoomId(org.mockito.ArgumentMatchers.anyLong());
            verify(chatFileRepository, never()).findObjectKeysByChatRoomId(org.mockito.ArgumentMatchers.anyLong());
            verify(objectStorageDeleteOutboxService, never()).enqueuePartyChatFiles(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.any()
            );
            verify(chatFileRepository, never()).deleteByChatRoomId(org.mockito.ArgumentMatchers.anyLong());
            verify(chatMessageRepository, never()).deleteByChatRoomId(org.mockito.ArgumentMatchers.anyLong());
            verify(chatRoomMemberRepository, never()).deleteByChatRoomId(org.mockito.ArgumentMatchers.anyLong());
            verify(chatRoomRepository, never()).deleteRoomById(org.mockito.ArgumentMatchers.anyLong());
            verify(applicationEventPublisher, never()).publishEvent(any(ChatRoomRedisCleanupEvent.class));
        }
    }
}
