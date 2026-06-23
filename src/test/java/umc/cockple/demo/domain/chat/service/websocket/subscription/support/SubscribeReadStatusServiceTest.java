package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatMessageUnreadCountDTO;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeReadStatusService")
class SubscribeReadStatusServiceTest {

    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SubscribeReadStatusService subscribeReadStatusService;

    @BeforeEach
    void setUp() {
        subscribeReadStatusService = new SubscribeReadStatusService(
                messageReadStatusRepository,
                chatRoomMemberRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("구독 시 unread 메시지를 읽음 처리하면 안읽음 상태 업데이트 이벤트를 발행한다")
    void markUnreadMessagesAsReadOnSubscribe_publishesUnreadStatusUpdateEvent() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        Long firstMessageId = 201L;
        Long secondMessageId = 202L;

        ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        Member member = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", memberId);
        ChatRoomMember chatRoomMember = ChatFixture.createJoinedMemberWithLastRead(chatRoom, member, firstMessageId - 1);

        given(messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId))
                .willReturn(List.of(firstMessageId, secondMessageId));
        given(messageReadStatusRepository.markMessagesAsReadForMember(
                chatRoomId, memberId, List.of(firstMessageId, secondMessageId))).willReturn(2);
        given(messageReadStatusRepository.countUnreadByMessageIds(List.of(firstMessageId, secondMessageId)))
                .willReturn(List.of(
                        new ChatMessageUnreadCountDTO(firstMessageId, 2L),
                        new ChatMessageUnreadCountDTO(secondMessageId, 1L)
                ));
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId))
                .willReturn(Optional.of(chatRoomMember));

        // when
        List<SubscribeReadStatusService.MessageUnreadUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates)
                .extracting(SubscribeReadStatusService.MessageUnreadUpdate::messageId)
                .containsExactly(firstMessageId, secondMessageId);
        assertThat(updates)
                .extracting(SubscribeReadStatusService.MessageUnreadUpdate::newUnreadCount)
                .containsExactly(2, 1);
        assertThat(chatRoomMember.getLastReadMessageId()).isEqualTo(secondMessageId);
        then(messageReadStatusRepository).should()
                .markMessagesAsReadForMember(chatRoomId, memberId, List.of(firstMessageId, secondMessageId));
        then(messageReadStatusRepository).should()
                .countUnreadByMessageIds(List.of(firstMessageId, secondMessageId));

        ArgumentCaptor<ChatUnreadStatusUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatUnreadStatusUpdateEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().targetMemberIds()).containsExactly(memberId);
    }

    @Test
    @DisplayName("구독 시 unread 메시지가 없으면 안읽음 상태 업데이트 이벤트를 발행하지 않는다")
    void markUnreadMessagesAsReadOnSubscribe_doesNotPublishEventWhenNoUnreadMessages() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        given(messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId))
                .willReturn(List.of());

        // when
        List<SubscribeReadStatusService.MessageUnreadUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates).isEmpty();
        then(messageReadStatusRepository).should(never())
                .markMessagesAsReadForMember(anyLong(), anyLong(), anyList());
        then(messageReadStatusRepository).should(never()).countUnreadByMessageIds(anyList());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("구독 시 메시지별 안읽음 수 결과가 누락되면 0으로 반환한다")
    void markUnreadMessagesAsReadOnSubscribe_fillsMissingUnreadCountWithZero() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        Long firstMessageId = 201L;
        Long secondMessageId = 202L;

        ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        Member member = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", memberId);
        ChatRoomMember chatRoomMember = ChatFixture.createJoinedMemberWithLastRead(chatRoom, member, firstMessageId - 1);

        given(messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId))
                .willReturn(List.of(firstMessageId, secondMessageId));
        given(messageReadStatusRepository.markMessagesAsReadForMember(
                chatRoomId, memberId, List.of(firstMessageId, secondMessageId))).willReturn(2);
        given(messageReadStatusRepository.countUnreadByMessageIds(List.of(firstMessageId, secondMessageId)))
                .willReturn(List.of(new ChatMessageUnreadCountDTO(firstMessageId, 2L)));
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId))
                .willReturn(Optional.of(chatRoomMember));

        // when
        List<SubscribeReadStatusService.MessageUnreadUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates)
                .extracting(SubscribeReadStatusService.MessageUnreadUpdate::messageId)
                .containsExactly(firstMessageId, secondMessageId);
        assertThat(updates)
                .extracting(SubscribeReadStatusService.MessageUnreadUpdate::newUnreadCount)
                .containsExactly(2, 0);
    }
}
