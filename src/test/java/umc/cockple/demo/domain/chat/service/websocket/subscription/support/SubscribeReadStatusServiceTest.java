package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatMessageUnreadCountDTO;
import umc.cockple.demo.domain.chat.service.websocket.UnreadCountUpdate;

import java.util.List;

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

        given(messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId))
                .willReturn(List.of(firstMessageId, secondMessageId));
        given(messageReadStatusRepository.markMessagesAsReadForMember(
                chatRoomId, memberId, List.of(firstMessageId, secondMessageId))).willReturn(2);
        given(messageReadStatusRepository.countUnreadByMessageIds(List.of(firstMessageId, secondMessageId)))
                .willReturn(List.of(
                        new ChatMessageUnreadCountDTO(firstMessageId, 2L),
                        new ChatMessageUnreadCountDTO(secondMessageId, 1L)
                ));
        given(chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, secondMessageId))
                .willReturn(1);

        // when
        List<UnreadCountUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates)
                .extracting(UnreadCountUpdate::messageId)
                .containsExactly(firstMessageId, secondMessageId);
        assertThat(updates)
                .extracting(UnreadCountUpdate::newUnreadCount)
                .containsExactly(2, 1);
        then(messageReadStatusRepository).should()
                .markMessagesAsReadForMember(chatRoomId, memberId, List.of(firstMessageId, secondMessageId));
        then(messageReadStatusRepository).should()
                .countUnreadByMessageIds(List.of(firstMessageId, secondMessageId));
        then(chatRoomMemberRepository).should()
                .advanceLastReadMessageId(chatRoomId, memberId, secondMessageId);

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
        List<UnreadCountUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates).isEmpty();
        then(messageReadStatusRepository).should(never())
                .markMessagesAsReadForMember(anyLong(), anyLong(), anyList());
        then(messageReadStatusRepository).should(never()).countUnreadByMessageIds(anyList());
        then(chatRoomMemberRepository).should(never()).advanceLastReadMessageId(anyLong(), anyLong(), anyLong());
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

        given(messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId))
                .willReturn(List.of(firstMessageId, secondMessageId));
        given(messageReadStatusRepository.markMessagesAsReadForMember(
                chatRoomId, memberId, List.of(firstMessageId, secondMessageId))).willReturn(2);
        given(messageReadStatusRepository.countUnreadByMessageIds(List.of(firstMessageId, secondMessageId)))
                .willReturn(List.of(new ChatMessageUnreadCountDTO(firstMessageId, 2L)));
        given(chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, secondMessageId))
                .willReturn(1);

        // when
        List<UnreadCountUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates)
                .extracting(UnreadCountUpdate::messageId)
                .containsExactly(firstMessageId, secondMessageId);
        assertThat(updates)
                .extracting(UnreadCountUpdate::newUnreadCount)
                .containsExactly(2, 0);
    }
}
