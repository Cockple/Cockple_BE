package umc.cockple.demo.domain.chat.service.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;
import umc.cockple.demo.domain.chat.presentation.websocket.session.ChatWebSocketSessionRegistry;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.websocket.send.support.ChatSendEventPublisher;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;

@DisplayName("ChatUnreadStatusUpdateEvent after-commit flow")
// 채팅 @Async 풀(chatExecutor)을 SyncTaskExecutor로 덮어써 동기 실행하려면 빈 오버라이드 허용 필요
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class ChatUnreadStatusUpdateEventFlowTest extends IntegrationTestBase {

    // handleChatUnreadStatusUpdate의 @Async("chatExecutor")를 동기로 실행해 테스트 타이밍 문제를 제거
    @TestConfiguration
    static class SyncAsyncConfig {

        @Bean
        public TaskExecutor chatExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private MessageReadStatusRepository messageReadStatusRepository;
    @Autowired private ChatWebSocketSessionRegistry sessionRegistry;
    @Autowired private ChatSendEventPublisher chatSendEventPublisher;
    @Autowired private SubscribeReadStatusService subscribeReadStatusService;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @MockitoBean private ChatMessageSender chatMessageSender;

    private final List<RegisteredSession> registeredSessions = new ArrayList<>();

    @AfterEach
    void tearDown() {
        registeredSessions.forEach(registered -> sessionRegistry.remove(registered.memberId(), registered.session()));
        registeredSessions.clear();
        reset(chatMessageSender);

        messageReadStatusRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("메시지 전송 unread 이벤트는 커밋 후 unread-status WebSocket payload를 전송한다")
    void sendUnreadStatusEvent_afterCommit_sendsUnreadStatusPayload() {
        // given
        Member sender = memberRepository.save(MemberFixture.createMember("발신자", Gender.MALE, Level.A, 1001L));
        Member receiver = memberRepository.save(MemberFixture.createMember("수신자", Gender.FEMALE, Level.B, 2002L));
        ChatRoom chatRoom = createPartyChatRoom(sender, receiver);

        ChatMessage unreadMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(chatRoom, sender, "읽지 않은 모임 메시지"));
        messageReadStatusRepository.save(
                MessageReadStatus.createUnread(unreadMessage.getId(), receiver.getId(), chatRoom.getId()));
        registerOpenSession(receiver.getId());

        // when
        transactionTemplate.execute(status -> {
            chatSendEventPublisher.publishUnreadStatusUpdateEvent(chatRoom, sender.getId());
            return null;
        });

        // then
        ArgumentCaptor<EncodedRealtimeMessage> messageCaptor = ArgumentCaptor.forClass(EncodedRealtimeMessage.class);
        then(chatMessageSender).should(times(1)).send(eq(receiver.getId()), messageCaptor.capture());

        assertThat(messageCaptor.getValue().payload())
                .contains("\"type\":\"UNREAD_STATUS_UPDATE\"")
                .contains("\"hasUnread\":true")
                .contains("\"hasPartyUnread\":true")
                .contains("\"hasDirectUnread\":false");
    }

    @Test
    @DisplayName("구독 읽음 처리 unread 이벤트는 커밋 후 최신 unread-status WebSocket payload를 전송한다")
    void subscribeUnreadStatusEvent_afterCommit_sendsUnreadStatusPayload() {
        // given
        Member subscriber = memberRepository.save(MemberFixture.createMember("구독자", Gender.MALE, Level.A, 1001L));
        Member sender = memberRepository.save(MemberFixture.createMember("상대방", Gender.FEMALE, Level.B, 2002L));
        ChatRoom chatRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
        chatRoomMemberRepository.save(ChatRoomMember.createJoined(chatRoom, subscriber, "상대방 채팅"));
        chatRoomMemberRepository.save(ChatRoomMember.createJoined(chatRoom, sender, "구독자 채팅"));

        ChatMessage unreadMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(chatRoom, sender, "구독 시 읽음 처리될 메시지"));
        messageReadStatusRepository.save(
                MessageReadStatus.createUnread(unreadMessage.getId(), subscriber.getId(), chatRoom.getId()));
        registerOpenSession(subscriber.getId());

        // when
        transactionTemplate.execute(status -> {
            subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoom.getId(), subscriber.getId());
            return null;
        });

        // then
        ArgumentCaptor<EncodedRealtimeMessage> messageCaptor = ArgumentCaptor.forClass(EncodedRealtimeMessage.class);
        then(chatMessageSender).should(times(1)).send(eq(subscriber.getId()), messageCaptor.capture());

        assertThat(messageCaptor.getValue().payload())
                .contains("\"type\":\"UNREAD_STATUS_UPDATE\"")
                .contains("\"hasUnread\":false")
                .contains("\"hasPartyUnread\":false")
                .contains("\"hasDirectUnread\":false");
    }

    private ChatRoom createPartyChatRoom(Member sender, Member receiver) {
        PartyAddr partyAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        Party party = partyRepository.save(PartyFixture.createParty("배드민턴 모임", sender.getId(), partyAddr));
        ChatRoom chatRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(party));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, sender));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, receiver));
        return chatRoom;
    }

    private void registerOpenSession(Long memberId) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("chat-session-" + memberId);
        given(session.isOpen()).willReturn(true);
        sessionRegistry.register(memberId, session);
        registeredSessions.add(new RegisteredSession(memberId, session));
    }

    private record RegisteredSession(Long memberId, WebSocketSession session) {
    }
}
