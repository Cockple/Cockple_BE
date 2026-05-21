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
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.websocket.ChatReadService;
import umc.cockple.demo.domain.chat.service.websocket.ChatSendService;
import umc.cockple.demo.domain.chat.service.websocket.MessageReadCreationService;
import umc.cockple.demo.domain.chat.service.websocket.SubscriptionService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private MessageReadCreationService messageReadCreationService;
    @Mock private ChatProcessor chatProcessor;
    @Mock private ChatReadService chatReadService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ChatSendService chatSendService;
    private ChatConverter chatConverter;

    @BeforeEach
    void setUp() {
        chatConverter = new ChatConverter();
        chatSendService = new ChatSendService(
                chatRoomRepository,
                memberRepository,
                chatMessageRepository,
                chatRoomMemberRepository,
                messageReadStatusRepository,
                subscriptionService,
                messageReadCreationService,
                chatProcessor,
                chatConverter,
                chatReadService,
                eventPublisher
        );
    }

    @Test
    @DisplayName("시스템 메시지를 저장하면 읽음 상태, 브로드캐스트, 채팅방 목록 업데이트를 함께 처리한다")
    void sendSystemMessage_publishesReadStatusBroadcastAndListUpdate() {
        // given
        Long partyId = 10L;
        Long roomId = 20L;
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 21, 13, 15);
        String content = "홍길동님이 모임에 참여하셨습니다.";

        Member memberA = MemberFixture.createMemberWithName("홍길동", "길동", umc.cockple.demo.global.enums.Gender.MALE, umc.cockple.demo.global.enums.Level.A, 1001L);
        ReflectionTestUtils.setField(memberA, "id", 101L);
        Member memberB = MemberFixture.createMemberWithName("김철수", "철수", umc.cockple.demo.global.enums.Gender.MALE, umc.cockple.demo.global.enums.Level.B, 1002L);
        ReflectionTestUtils.setField(memberB, "id", 102L);

        Party party = PartyFixture.createParty("배드민턴 모임", 101L, PartyFixture.createPartyAddr("서울", "강남구"));
        ReflectionTestUtils.setField(party, "id", partyId);

        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        ChatRoomMember memberOne = ChatFixture.createJoinedMember(chatRoom, memberA);
        ChatRoomMember memberTwo = ChatFixture.createJoinedMember(chatRoom, memberB);

        given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 300L);
            ReflectionTestUtils.setField(savedMessage, "createdAt", sentAt);
            return savedMessage;
        });
        given(subscriptionService.getActiveSubscribers(roomId)).willReturn(List.of(101L));
        given(chatReadService.subscribersToReadStatus(eq(roomId), anyLong(), eq(List.of(101L)), isNull())).willReturn(1);
        given(chatRoomMemberRepository.findMemberIdsByChatRoomId(roomId)).willReturn(List.of(101L, 102L));
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, 101L)).willReturn(Optional.of(memberOne));
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, 102L)).willReturn(Optional.of(memberTwo));
        given(messageReadStatusRepository.countAllUnreadMessages(roomId, 101L)).willReturn(0);
        given(messageReadStatusRepository.countAllUnreadMessages(roomId, 102L)).willReturn(1);

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
}
