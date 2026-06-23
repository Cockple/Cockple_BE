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
import umc.cockple.demo.domain.chat.service.support.reader.ReadStatusReader;
import umc.cockple.demo.domain.chat.service.support.updater.ChatMemberReadStateUpdater;
import umc.cockple.demo.domain.chat.service.support.updater.ReadStatusUpdater;
import umc.cockple.demo.domain.chat.service.websocket.UnreadCountUpdate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeReadStatusService")
class SubscribeReadStatusServiceTest {

    @Mock private ReadStatusReader readStatusReader;
    @Mock private ReadStatusUpdater readStatusUpdater;
    @Mock private ChatMemberReadStateUpdater chatMemberReadStateUpdater;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SubscribeReadStatusService subscribeReadStatusService;

    @BeforeEach
    void setUp() {
        subscribeReadStatusService = new SubscribeReadStatusService(
                readStatusReader,
                readStatusUpdater,
                chatMemberReadStateUpdater,
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

        given(readStatusReader.findUnreadMessageIds(chatRoomId, memberId))
                .willReturn(List.of(firstMessageId, secondMessageId));
        given(readStatusUpdater.markMessagesAsReadForMember(
                chatRoomId, memberId, List.of(firstMessageId, secondMessageId))).willReturn(2);
        given(readStatusReader.countUnreadByMessageIdsAsSparseMap(List.of(firstMessageId, secondMessageId)))
                .willReturn(Map.of(firstMessageId, 2, secondMessageId, 1));
        given(chatMemberReadStateUpdater.advanceLastReadMessageId(chatRoomId, memberId, secondMessageId))
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
        then(readStatusUpdater).should()
                .markMessagesAsReadForMember(chatRoomId, memberId, List.of(firstMessageId, secondMessageId));
        then(readStatusReader).should()
                .countUnreadByMessageIdsAsSparseMap(List.of(firstMessageId, secondMessageId));
        then(chatMemberReadStateUpdater).should()
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
        given(readStatusReader.findUnreadMessageIds(chatRoomId, memberId))
                .willReturn(List.of());

        // when
        List<UnreadCountUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        assertThat(updates).isEmpty();
        then(readStatusUpdater).should(never())
                .markMessagesAsReadForMember(anyLong(), anyLong(), anyList());
        then(readStatusReader).should(never()).countUnreadByMessageIdsAsSparseMap(anyList());
        then(chatMemberReadStateUpdater).should(never()).advanceLastReadMessageId(anyLong(), anyLong(), anyLong());
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

        given(readStatusReader.findUnreadMessageIds(chatRoomId, memberId))
                .willReturn(List.of(firstMessageId, secondMessageId));
        given(readStatusUpdater.markMessagesAsReadForMember(
                chatRoomId, memberId, List.of(firstMessageId, secondMessageId))).willReturn(2);
        given(readStatusReader.countUnreadByMessageIdsAsSparseMap(List.of(firstMessageId, secondMessageId)))
                .willReturn(Map.of(firstMessageId, 2));
        given(chatMemberReadStateUpdater.advanceLastReadMessageId(chatRoomId, memberId, secondMessageId))
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

    @Test
    @DisplayName("구독 시 unread 메시지 ID 목록이 정렬되지 않아도 가장 큰 메시지 ID로 lastReadMessageId를 갱신한다")
    void markUnreadMessagesAsReadOnSubscribe_advancesLastReadToMaxMessageId() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        Long latestMessageId = 202L;
        Long olderMessageId = 201L;
        List<Long> unorderedMessageIds = List.of(latestMessageId, olderMessageId);

        given(readStatusReader.findUnreadMessageIds(chatRoomId, memberId))
                .willReturn(unorderedMessageIds);
        given(readStatusUpdater.markMessagesAsReadForMember(chatRoomId, memberId, unorderedMessageIds))
                .willReturn(2);
        given(readStatusReader.countUnreadByMessageIdsAsSparseMap(unorderedMessageIds))
                .willReturn(Map.of(latestMessageId, 1, olderMessageId, 1));
        given(chatMemberReadStateUpdater.advanceLastReadMessageId(chatRoomId, memberId, latestMessageId))
                .willReturn(1);

        // when
        subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        // then
        then(chatMemberReadStateUpdater).should()
                .advanceLastReadMessageId(chatRoomId, memberId, latestMessageId);
    }
}
