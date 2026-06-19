package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatWebSocketResponseAssembler;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.service.support.ChatMessageFileAppender;
import umc.cockple.demo.domain.chat.service.support.ChatSendEventPublisher;
import umc.cockple.demo.domain.chat.service.support.DirectChatRoomActivationService;
import umc.cockple.demo.domain.chat.service.support.reader.ChatMemberReader;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomReader;
import umc.cockple.demo.domain.chat.service.websocket.ChatReadService;
import umc.cockple.demo.domain.chat.service.websocket.ChatSendService;
import umc.cockple.demo.domain.chat.service.websocket.MessageReadCreationService;
import umc.cockple.demo.domain.chat.service.websocket.SubscriptionService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSendService")
class ChatSendServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomReader chatRoomReader;
    @Mock private ChatMemberReader chatMemberReader;
    @Mock private ChatMessageFileAppender chatMessageFileAppender;
    @Mock private DirectChatRoomActivationService directChatRoomActivationService;
    @Mock private ChatSendEventPublisher chatSendEventPublisher;
    @Mock private SubscriptionService subscriptionService;
    @Mock private MessageReadCreationService messageReadCreationService;
    @Mock private ChatProcessor chatProcessor;
    @Mock private ChatReadService chatReadService;

    private ChatSendService chatSendService;
    private ChatWebSocketResponseAssembler chatWebSocketResponseAssembler;

    @BeforeEach
    void setUp() {
        chatWebSocketResponseAssembler = new ChatWebSocketResponseAssembler();
        chatSendService = new ChatSendService(
                chatMessageRepository,
                chatRoomReader,
                chatMemberReader,
                chatMessageFileAppender,
                directChatRoomActivationService,
                chatSendEventPublisher,
                subscriptionService,
                messageReadCreationService,
                chatProcessor,
                chatWebSocketResponseAssembler,
                chatReadService
        );
    }

    @Test
    @DisplayName("일반 메시지를 저장하면 읽음 상태, 브로드캐스트, 후속 이벤트 발행을 위임한다")
    void sendMessage_savesBroadcastsAndDelegatesEvents() {
        // given
        Long roomId = 20L;
        Long senderId = 101L;
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 21, 13, 15);

        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", senderId);

        Party party = PartyFixture.createParty("배드민턴 모임", senderId, PartyFixture.createPartyAddr("서울", "강남구"));
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

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

        // when
        chatSendService.sendMessage(roomId, "안녕하세요", List.of(), senderId);

        // then
        ArgumentCaptor<ChatMessage> savedMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        then(chatMessageRepository).should().save(savedMessageCaptor.capture());
        ChatMessage savedMessage = savedMessageCaptor.getValue();
        assertThat(savedMessage.getType()).isEqualTo(MessageType.TEXT);
        assertThat(savedMessage.getSender()).isSameAs(sender);
        assertThat(savedMessage.getContent()).isEqualTo("안녕하세요");

        then(chatMessageFileAppender).should().append(savedMessage, List.of());
        then(directChatRoomActivationService).should().joinPendingMemberOnFirstMessage(chatRoom, senderId);
        then(messageReadCreationService).should().createReadStatusForNewMessage(savedMessage, senderId);
        then(chatReadService).should().subscribersToReadStatus(roomId, 300L, List.of(senderId), senderId);

        ArgumentCaptor<WebSocketMessageDTO.MessageResponse> messageResponseCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.MessageResponse.class);
        then(subscriptionService).should().broadcastMessage(eq(roomId), messageResponseCaptor.capture(), eq(senderId));
        assertThat(messageResponseCaptor.getValue().messageId()).isEqualTo(300L);
        assertThat(messageResponseCaptor.getValue().unreadCount()).isEqualTo(2);

        then(chatSendEventPublisher).should()
                .publishChatNotificationEvent(chatRoom, savedMessage, sender, List.of(senderId));
        then(chatSendEventPublisher).should().publishChatRoomListUpdateEvent(chatRoom, savedMessage);
        then(chatSendEventPublisher).should().publishUnreadStatusUpdateEvent(chatRoom, senderId);
    }

    @Test
    @DisplayName("시스템 메시지를 저장하면 읽음 상태, 브로드캐스트, 채팅방 목록 업데이트 이벤트를 위임한다")
    void sendSystemMessage_savesBroadcastsAndDelegatesListUpdate() {
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

        then(chatSendEventPublisher).should().publishChatRoomListUpdateEvent(chatRoom, savedMessage);
    }
}
