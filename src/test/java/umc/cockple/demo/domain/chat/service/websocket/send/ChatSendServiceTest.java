package umc.cockple.demo.domain.chat.service.websocket.send;

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
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.service.support.assembler.ChatMessageViewAssembler;
import umc.cockple.demo.domain.chat.service.websocket.send.support.ChatMessageFileAppender;
import umc.cockple.demo.domain.chat.service.websocket.send.support.ChatSendEventPublisher;
import umc.cockple.demo.domain.chat.service.websocket.send.support.DirectChatRoomActivationService;
import umc.cockple.demo.domain.chat.service.support.reader.ChatMemberReader;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomReader;
import umc.cockple.demo.domain.chat.service.websocket.send.support.SentMessageReadStatusService;
import umc.cockple.demo.domain.chat.service.websocket.send.ChatSendService;
import umc.cockple.demo.domain.chat.service.websocket.send.support.MessageReadCreationService;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomMessageBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.ActiveChatRoomSubscriberReader;
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
    @Mock private ActiveChatRoomSubscriberReader activeChatRoomSubscriberReader;
    @Mock private ChatRoomMessageBroadcaster chatRoomMessageBroadcaster;
    @Mock private MessageReadCreationService messageReadCreationService;
    @Mock private ChatMessageViewAssembler chatMessageViewAssembler;
    @Mock private SentMessageReadStatusService sentMessageReadStatusService;

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
                activeChatRoomSubscriberReader,
                chatRoomMessageBroadcaster,
                messageReadCreationService,
                chatMessageViewAssembler,
                chatWebSocketResponseAssembler,
                sentMessageReadStatusService
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
        given(chatMessageViewAssembler.generateProfileImageUrl(isNull())).willReturn("https://cdn.example.com/profile");
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 300L);
            ReflectionTestUtils.setField(savedMessage, "createdAt", sentAt);
            return savedMessage;
        });
        given(activeChatRoomSubscriberReader.findActiveSubscribers(roomId)).willReturn(List.of(senderId));
        given(sentMessageReadStatusService.markActiveSubscribersAsRead(roomId, 300L, List.of(senderId), senderId)).willReturn(2);

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
        then(sentMessageReadStatusService).should().markActiveSubscribersAsRead(roomId, 300L, List.of(senderId), senderId);

        ArgumentCaptor<WebSocketMessageDTO.MessageResponse> messageResponseCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.MessageResponse.class);
        then(chatRoomMessageBroadcaster).should()
                .broadcast(eq(roomId), messageResponseCaptor.capture(), eq(List.of(senderId)), eq(senderId));
        assertThat(messageResponseCaptor.getValue().messageId()).isEqualTo(300L);
        assertThat(messageResponseCaptor.getValue().unreadCount()).isEqualTo(2);

        then(chatSendEventPublisher).should()
                .publishChatNotificationEvent(chatRoom, savedMessage, sender, List.of(senderId));
        then(chatSendEventPublisher).should().publishChatRoomListUpdateEvent(chatRoom, savedMessage);
        then(chatSendEventPublisher).should().publishUnreadStatusUpdateEvent(chatRoom, senderId);
    }

    @Test
    @DisplayName("일반 메시지 응답에는 프로필 URL과 저장된 파일 정보가 저장 순서대로 포함된다")
    void sendMessage_responseContainsProfileAndFilesInStoredOrder() {
        // given
        Long roomId = 20L;
        Long senderId = 101L;
        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", senderId);

        Party party = PartyFixture.createParty("배드민턴 모임", senderId, PartyFixture.createPartyAddr("서울", "강남구"));
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        List<WebSocketMessageDTO.Request.FileInfo> requestFiles = List.of(
                WebSocketMessageDTO.Request.FileInfo.builder()
                        .imgKey("chat/second.jpg")
                        .imgOrder(2)
                        .originalFileName("second.jpg")
                        .fileSize(200L)
                        .fileType("image/jpeg")
                        .build(),
                WebSocketMessageDTO.Request.FileInfo.builder()
                        .imgKey("chat/first.jpg")
                        .imgOrder(1)
                        .originalFileName("first.jpg")
                        .fileSize(100L)
                        .fileType("image/jpeg")
                        .build()
        );

        given(chatRoomReader.read(roomId)).willReturn(chatRoom);
        given(chatMemberReader.readWithProfile(senderId)).willReturn(sender);
        given(chatMessageViewAssembler.generateProfileImageUrl(isNull())).willReturn("https://cdn.example.com/profile");
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 300L);

            ChatMessageFile secondFile = ChatMessageFile.create(
                    savedMessage, "chat/second.jpg", 2, "second.jpg", 200L, "image/jpeg");
            ChatMessageFile firstFile = ChatMessageFile.create(
                    savedMessage, "chat/first.jpg", 1, "first.jpg", 100L, "image/jpeg");
            ReflectionTestUtils.setField(secondFile, "id", 302L);
            ReflectionTestUtils.setField(firstFile, "id", 301L);
            ReflectionTestUtils.setField(savedMessage, "chatMessageFiles", List.of(secondFile, firstFile));
            return savedMessage;
        });
        given(chatMessageViewAssembler.generateFileUrl(any(ChatMessageFile.class))).willAnswer(invocation -> {
            ChatMessageFile file = invocation.getArgument(0);
            return "https://cdn.example.com/" + file.getFileKey();
        });
        given(activeChatRoomSubscriberReader.findActiveSubscribers(roomId)).willReturn(List.of(senderId));
        given(sentMessageReadStatusService.markActiveSubscribersAsRead(roomId, 300L, List.of(senderId), senderId))
                .willReturn(2);

        // when
        chatSendService.sendMessage(roomId, "사진입니다", requestFiles, senderId);

        // then
        then(chatMessageFileAppender).should().append(any(ChatMessage.class), eq(requestFiles));

        ArgumentCaptor<WebSocketMessageDTO.MessageResponse> responseCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.MessageResponse.class);
        then(chatRoomMessageBroadcaster).should()
                .broadcast(eq(roomId), responseCaptor.capture(), eq(List.of(senderId)), eq(senderId));

        WebSocketMessageDTO.MessageResponse response = responseCaptor.getValue();
        assertThat(response.senderProfileImageUrl()).isEqualTo("https://cdn.example.com/profile");
        assertThat(response.images()).extracting(image -> image.imageId())
                .containsExactly(302L, 301L);
        assertThat(response.images()).extracting(image -> image.imageUrl())
                .containsExactly(
                        "https://cdn.example.com/chat/second.jpg",
                        "https://cdn.example.com/chat/first.jpg"
                );
        assertThat(response.images()).extracting(image -> image.imgOrder())
                .containsExactly(2, 1);
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
        given(activeChatRoomSubscriberReader.findActiveSubscribers(roomId)).willReturn(List.of(101L));
        given(sentMessageReadStatusService.markActiveSubscribersAsRead(eq(roomId), anyLong(), eq(List.of(101L)), isNull())).willReturn(1);

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
        then(sentMessageReadStatusService).should().markActiveSubscribersAsRead(roomId, 300L, List.of(101L), null);

        ArgumentCaptor<WebSocketMessageDTO.MessageResponse> messageResponseCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.MessageResponse.class);
        then(chatRoomMessageBroadcaster).should()
                .broadcast(eq(roomId), messageResponseCaptor.capture(), eq(List.of(101L)), isNull());
        WebSocketMessageDTO.MessageResponse response = messageResponseCaptor.getValue();
        assertThat(response.chatRoomId()).isEqualTo(roomId);
        assertThat(response.messageId()).isEqualTo(300L);
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.messageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(response.senderId()).isNull();
        assertThat(response.senderName()).isEqualTo("시스템");

        then(chatSendEventPublisher).should().publishChatRoomListUpdateEvent(chatRoom, savedMessage);
        then(chatSendEventPublisher).should().publishUnreadStatusUpdateEvent(chatRoom, null);
    }
}
