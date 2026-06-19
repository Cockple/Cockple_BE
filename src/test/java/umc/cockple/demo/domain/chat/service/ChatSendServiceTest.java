package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatWebSocketResponseAssembler;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatMemberUnreadCountDTO;
import umc.cockple.demo.domain.chat.service.websocket.ChatReadService;
import umc.cockple.demo.domain.chat.service.websocket.ChatSendService;
import umc.cockple.demo.domain.chat.service.websocket.MessageReadCreationService;
import umc.cockple.demo.domain.chat.service.websocket.SubscriptionService;
import umc.cockple.demo.domain.chat.service.support.reader.ChatMemberReader;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSendService")
class ChatSendServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomReader chatRoomReader;
    @Mock private ChatMemberReader chatMemberReader;
    @Mock private SubscriptionService subscriptionService;
    @Mock private MessageReadCreationService messageReadCreationService;
    @Mock private ChatProcessor chatProcessor;
    @Mock private ChatReadService chatReadService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ChatSendService chatSendService;
    private ChatWebSocketResponseAssembler chatWebSocketResponseAssembler;
    private ChatUnreadQueryService chatUnreadQueryService;

    @BeforeEach
    void setUp() {
        chatWebSocketResponseAssembler = new ChatWebSocketResponseAssembler();
        chatUnreadQueryService = new ChatUnreadQueryService(messageReadStatusRepository);
        chatSendService = new ChatSendService(
                chatMessageRepository,
                chatRoomMemberRepository,
                chatRoomReader,
                chatMemberReader,
                subscriptionService,
                messageReadCreationService,
                chatProcessor,
                chatWebSocketResponseAssembler,
                chatReadService,
                chatUnreadQueryService,
                eventPublisher
        );
    }

    @Test
    @DisplayName("일반 메시지 전송 후 발신자를 제외한 참여자에게 안읽음 상태 업데이트 이벤트를 발행한다")
    void sendMessage_publishesUnreadStatusUpdateEventForReceivers() {
        // given
        Long roomId = 20L;
        Long senderId = 101L;
        Long receiverId = 102L;
        Long anotherReceiverId = 103L;
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 21, 13, 15);

        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", senderId);

        Party party = PartyFixture.createParty("배드민턴 모임", senderId, PartyFixture.createPartyAddr("서울", "강남구"));
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        List<Long> roomMemberIds = List.of(senderId, receiverId, anotherReceiverId);

        given(chatRoomReader.read(roomId)).willReturn(chatRoom);
        given(chatMemberReader.readWithProfile(senderId)).willReturn(sender);
        given(chatProcessor.generateProfileImageUrl(isNull())).willReturn("https://cdn.example.com/profile");
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 300L);
            ReflectionTestUtils.setField(savedMessage, "createdAt", sentAt);
            return savedMessage;
        });
        given(subscriptionService.getActiveSubscribers(roomId)).willReturn(List.of(senderId));
        given(chatReadService.subscribersToReadStatus(roomId, 300L, List.of(senderId), senderId)).willReturn(2);
        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId)).willReturn(roomMemberIds);
        given(messageReadStatusRepository.countUnreadMessagesByMembers(roomId, roomMemberIds))
                .willReturn(List.of(
                        new ChatMemberUnreadCountDTO(receiverId, 1L),
                        new ChatMemberUnreadCountDTO(anotherReceiverId, 1L)
                ));

        // when
        chatSendService.sendMessage(roomId, "안녕하세요", List.of(), senderId);

        // then
        ArgumentCaptor<WebSocketMessageDTO.MessageResponse> messageResponseCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.MessageResponse.class);
        then(subscriptionService).should().broadcastMessage(eq(roomId), messageResponseCaptor.capture(), eq(senderId));
        assertThat(messageResponseCaptor.getValue().messageId()).isEqualTo(300L);
        assertThat(messageResponseCaptor.getValue().unreadCount()).isEqualTo(2);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(times(3)).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues())
                .filteredOn(ChatUnreadStatusUpdateEvent.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    ChatUnreadStatusUpdateEvent unreadEvent = (ChatUnreadStatusUpdateEvent) event;
                    assertThat(unreadEvent.targetMemberIds()).containsExactly(receiverId, anotherReceiverId);
                });
    }

    @Test
    @DisplayName("시스템 메시지를 저장하면 읽음 상태, 브로드캐스트, 채팅방 목록 업데이트를 함께 처리한다")
    void sendSystemMessage_publishesReadStatusBroadcastAndListUpdate() {
        // given
        Long partyId = 10L;
        Long roomId = 20L;
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 21, 13, 15);
        String content = "홍길동님이 모임에 참여하셨습니다.";

        Party party = PartyFixture.createParty("배드민턴 모임", 101L, PartyFixture.createPartyAddr("서울", "강남구"));
        ReflectionTestUtils.setField(party, "id", partyId);

        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        given(chatRoomReader.readByPartyId(partyId)).willReturn(chatRoom);
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 300L);
            ReflectionTestUtils.setField(savedMessage, "createdAt", sentAt);
            return savedMessage;
        });
        given(subscriptionService.getActiveSubscribers(roomId)).willReturn(List.of(101L));
        given(chatReadService.subscribersToReadStatus(eq(roomId), anyLong(), eq(List.of(101L)), isNull())).willReturn(1);
        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId)).willReturn(List.of(101L, 102L));
        given(messageReadStatusRepository.countUnreadMessagesByMembers(roomId, List.of(101L, 102L)))
                .willReturn(List.of(new ChatMemberUnreadCountDTO(102L, 1L)));

        // when
        chatSendService.sendSystemMessage(partyId, content);

        // then
        ArgumentCaptor<ChatMessage> savedMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        then(chatMessageRepository).should().save(savedMessageCaptor.capture());
        ChatMessage savedMessage = savedMessageCaptor.getValue();
        assertThat(savedMessage.getType()).isEqualTo(MessageType.SYSTEM);
        assertThat(savedMessage.getSender()).isNull();
        assertThat(savedMessage.getContent()).isEqualTo(content);

        then(messageReadCreationService).should().createReadStatusForNewMessage(savedMessage, null);
        then(chatReadService).should().subscribersToReadStatus(roomId, 300L, List.of(101L), null);

        ArgumentCaptor<WebSocketMessageDTO.MessageResponse> messageResponseCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.MessageResponse.class);
        then(subscriptionService).should().broadcastSystemMessage(eq(roomId), messageResponseCaptor.capture());
        WebSocketMessageDTO.MessageResponse response = messageResponseCaptor.getValue();
        assertThat(response.chatRoomId()).isEqualTo(roomId);
        assertThat(response.messageId()).isEqualTo(300L);
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.messageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(response.senderId()).isNull();
        assertThat(response.senderName()).isEqualTo("시스템");

        ArgumentCaptor<ChatRoomListUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ChatRoomListUpdateEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatRoomListUpdateEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(roomId);
        assertThat(event.content()).isEqualTo(content);
        assertThat(event.timestamp()).isEqualTo(sentAt);
        assertThat(event.messageType()).isEqualTo(MessageType.SYSTEM.name());
        assertThat(event.memberUnreadCounts()).containsEntry(101L, 0).containsEntry(102L, 1);
    }

    @Test
    @DisplayName("채팅방 목록 unread count 계산에 실패하면 0으로 만든 이벤트를 발행하지 않는다")
    void sendSystemMessage_doesNotPublishListUpdate_whenUnreadCountBatchFails() {
        // given
        Long partyId = 10L;
        Long roomId = 20L;
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 21, 13, 15);
        String content = "홍길동님이 모임에 참여하셨습니다.";

        Party party = PartyFixture.createParty("배드민턴 모임", 101L, PartyFixture.createPartyAddr("서울", "강남구"));
        ReflectionTestUtils.setField(party, "id", partyId);

        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        given(chatRoomReader.readByPartyId(partyId)).willReturn(chatRoom);
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 300L);
            ReflectionTestUtils.setField(savedMessage, "createdAt", sentAt);
            return savedMessage;
        });
        given(subscriptionService.getActiveSubscribers(roomId)).willReturn(List.of(101L));
        given(chatReadService.subscribersToReadStatus(eq(roomId), anyLong(), eq(List.of(101L)), isNull())).willReturn(1);
        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId)).willReturn(List.of(101L, 102L));
        given(messageReadStatusRepository.countUnreadMessagesByMembers(roomId, List.of(101L, 102L)))
                .willThrow(new RuntimeException("batch unread count failed"));

        // when
        chatSendService.sendSystemMessage(partyId, content);

        // then
        then(eventPublisher).should(never()).publishEvent(any(ChatRoomListUpdateEvent.class));
    }
}
