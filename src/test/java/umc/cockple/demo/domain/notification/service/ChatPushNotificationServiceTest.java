package umc.cockple.demo.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.events.ChatNotificationEvent;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatPushNotificationService")
class ChatPushNotificationServiceTest {

    @InjectMocks
    private ChatPushNotificationService chatPushNotificationService;

    @Mock
    private FcmService fcmService;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private Member sender;
    private Member activeSubscriber;
    private Member offlineMember;

    @BeforeEach
    void setUp() {
        sender = MemberFixture.createMember("발신자", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", 1L);

        activeSubscriber = MemberFixture.createMember("활성구독자", Gender.FEMALE, Level.B, 1002L);
        ReflectionTestUtils.setField(activeSubscriber, "id", 2L);

        offlineMember = MemberFixture.createMember("오프라인멤버", Gender.MALE, Level.C, 1003L);
        ReflectionTestUtils.setField(offlineMember, "id", 3L);
    }

    @Nested
    @DisplayName("단체채팅")
    class PartyChatRoom {

        private ChatRoom chatRoom;

        @BeforeEach
        void setUp() {
            Party party = PartyFixture.createParty("테스트모임", sender.getId(),
                    PartyFixture.createPartyAddr("서울", "강남구"));
            chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", 10L);
        }

        @Test
        @DisplayName("오프라인 멤버에게 FCM을 전송한다")
        void sendPush_toOfflineMember() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.PARTY,
                    "테스트모임", "발신자: 안녕하세요!",
                    sender.getId(), List.of(sender.getId(), activeSubscriber.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, activeSubscriber),
                            ChatFixture.createJoinedMember(chatRoom, offlineMember)
                    ));

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should(times(1)).sendChatNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이벤트의 title과 content를 FCM에 그대로 전달한다")
        void sendPush_forwardsEventTitleAndContent() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.PARTY,
                    "테스트모임", "발신자: 안녕하세요!",
                    sender.getId(), List.of(sender.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, offlineMember)
                    ));

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should().sendChatNotification(
                    any(), titleCaptor.capture(), contentCaptor.capture(), any(), any()
            );
            assertThat(titleCaptor.getValue()).isEqualTo("테스트모임");
            assertThat(contentCaptor.getValue()).isEqualTo("발신자: 안녕하세요!");
        }

        @Test
        @DisplayName("sender는 FCM 대상에서 제외된다")
        void sendPush_excludesSender() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.PARTY,
                    "테스트모임", "발신자: 안녕하세요!",
                    sender.getId(), List.of()
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(ChatFixture.createJoinedMember(chatRoom, sender)));

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should(never()).sendChatNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("활성 구독자는 FCM 대상에서 제외된다")
        void sendPush_excludesActiveSubscribers() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.PARTY,
                    "테스트모임", "발신자: 안녕하세요!",
                    sender.getId(), List.of(activeSubscriber.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, activeSubscriber)
                    ));

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should(never()).sendChatNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("오프라인 멤버가 여러 명이면 모두에게 FCM을 전송한다")
        void sendPush_toMultipleOfflineMembers() {
            // given
            Member anotherOffline = MemberFixture.createMember("또다른오프라인", Gender.FEMALE, Level.A, 1004L);
            ReflectionTestUtils.setField(anotherOffline, "id", 4L);

            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.PARTY,
                    "테스트모임", "발신자: 공지사항입니다",
                    sender.getId(), List.of(sender.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, offlineMember),
                            ChatFixture.createJoinedMember(chatRoom, anotherOffline)
                    ));

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should(times(2)).sendChatNotification(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("개인채팅")
    class DirectChatRoom {

        private ChatRoom chatRoom;

        @BeforeEach
        void setUp() {
            chatRoom = ChatFixture.createDirectChatRoom();
            ReflectionTestUtils.setField(chatRoom, "id", 20L);
        }

        @Test
        @DisplayName("오프라인 상대방에게 FCM을 전송한다")
        void sendPush_toOfflineCounterPart() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.DIRECT,
                    "발신자", "안녕!",
                    sender.getId(), List.of(sender.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, offlineMember)
                    ));

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should(times(1)).sendChatNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이벤트의 title과 content를 FCM에 그대로 전달한다")
        void sendPush_forwardsEventTitleAndContent() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.DIRECT,
                    "발신자", "안녕!",
                    sender.getId(), List.of(sender.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, offlineMember)
                    ));

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should().sendChatNotification(
                    any(), titleCaptor.capture(), contentCaptor.capture(), any(), any()
            );
            assertThat(titleCaptor.getValue()).isEqualTo("발신자");
            assertThat(contentCaptor.getValue()).isEqualTo("안녕!");
        }

        @Test
        @DisplayName("상대방이 활성 구독자면 FCM을 전송하지 않는다")
        void sendPush_excludesActiveCounterPart() {
            // given
            ChatNotificationEvent event = ChatNotificationEvent.create(
                    chatRoom.getId(), ChatRoomType.DIRECT,
                    "발신자", "안녕!",
                    sender.getId(), List.of(offlineMember.getId())
            );
            given(chatRoomMemberRepository.findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED))
                    .willReturn(List.of(
                            ChatFixture.createJoinedMember(chatRoom, sender),
                            ChatFixture.createJoinedMember(chatRoom, offlineMember)
                    ));

            // when
            chatPushNotificationService.sendPush(event);

            // then
            then(fcmService).should(never()).sendChatNotification(any(), any(), any(), any(), any());
        }
    }
}
