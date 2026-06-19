package umc.cockple.demo.domain.chat.service.websocket.send.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.service.ChatUnreadQueryService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.events.ChatNotificationEvent;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSendEventPublisher")
class ChatSendEventPublisherTest {

    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatUnreadQueryService chatUnreadQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ChatSendEventPublisher chatSendEventPublisher;

    @BeforeEach
    void setUp() {
        chatSendEventPublisher =
                new ChatSendEventPublisher(chatRoomMemberRepository, chatUnreadQueryService, eventPublisher);
    }

    @Test
    @DisplayName("모임 채팅 알림 이벤트를 생성해 발행한다")
    void publishChatNotificationEvent_publishesPartyNotification() {
        // given
        Long roomId = 20L;
        Long senderId = 101L;
        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", senderId);

        Party party = PartyFixture.createParty("배드민턴 모임", senderId, PartyFixture.createPartyAddr("서울", "강남구"));
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "안녕하세요");

        // when
        chatSendEventPublisher.publishChatNotificationEvent(chatRoom, message, sender, List.of(senderId));

        // then
        ArgumentCaptor<ChatNotificationEvent> eventCaptor = ArgumentCaptor.forClass(ChatNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatNotificationEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(roomId);
        assertThat(event.chatRoomType()).isEqualTo(ChatRoomType.PARTY);
        assertThat(event.notificationTitle()).isEqualTo("배드민턴 모임");
        assertThat(event.notificationContent()).isEqualTo("길동: 안녕하세요");
        assertThat(event.senderId()).isEqualTo(senderId);
        assertThat(event.activeSubscriberIds()).containsExactly(senderId);
    }

    @Test
    @DisplayName("채팅방 목록 업데이트 이벤트를 unread count와 함께 발행한다")
    void publishChatRoomListUpdateEvent_publishesListUpdate() {
        // given
        Long roomId = 20L;
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 21, 13, 15);
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("배드민턴 모임", 101L, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        ChatMessage message = ChatFixture.createSystemMessage(chatRoom, "홍길동님이 모임에 참여하셨습니다.");
        ReflectionTestUtils.setField(message, "createdAt", sentAt);

        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId)).willReturn(List.of(101L, 102L));
        given(chatUnreadQueryService.countUnreadMessagesByMembers(roomId, List.of(101L, 102L)))
                .willReturn(Map.of(101L, 0, 102L, 1));

        // when
        chatSendEventPublisher.publishChatRoomListUpdateEvent(chatRoom, message);

        // then
        ArgumentCaptor<ChatRoomListUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ChatRoomListUpdateEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatRoomListUpdateEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(roomId);
        assertThat(event.content()).isEqualTo("홍길동님이 모임에 참여하셨습니다.");
        assertThat(event.timestamp()).isEqualTo(sentAt);
        assertThat(event.messageType()).isEqualTo(MessageType.SYSTEM.name());
        assertThat(event.memberUnreadCounts()).containsEntry(101L, 0).containsEntry(102L, 1);
    }

    @Test
    @DisplayName("채팅방 목록 unread count 계산에 실패하면 이벤트를 발행하지 않는다")
    void publishChatRoomListUpdateEvent_doesNotPublishWhenUnreadCountFails() {
        // given
        Long roomId = 20L;
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("배드민턴 모임", 101L, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        ReflectionTestUtils.setField(chatRoom, "id", roomId);
        ChatMessage message = ChatFixture.createSystemMessage(chatRoom, "시스템 메시지");

        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId)).willReturn(List.of(101L, 102L));
        given(chatUnreadQueryService.countUnreadMessagesByMembers(roomId, List.of(101L, 102L)))
                .willThrow(new RuntimeException("batch unread count failed"));

        // when
        chatSendEventPublisher.publishChatRoomListUpdateEvent(chatRoom, message);

        // then
        then(eventPublisher).should(never()).publishEvent(any(ChatRoomListUpdateEvent.class));
    }

    @Test
    @DisplayName("안읽음 상태 업데이트 이벤트는 발신자를 제외하고 중복을 제거해 발행한다")
    void publishUnreadStatusUpdateEvent_publishesTargetsExceptSender() {
        // given
        Long roomId = 20L;
        Long senderId = 101L;
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("배드민턴 모임", senderId, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        ReflectionTestUtils.setField(chatRoom, "id", roomId);
        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId))
                .willReturn(List.of(senderId, 102L, 102L, 103L));

        // when
        chatSendEventPublisher.publishUnreadStatusUpdateEvent(chatRoom, senderId);

        // then
        ArgumentCaptor<ChatUnreadStatusUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatUnreadStatusUpdateEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().targetMemberIds()).containsExactly(102L, 103L);
    }
}
