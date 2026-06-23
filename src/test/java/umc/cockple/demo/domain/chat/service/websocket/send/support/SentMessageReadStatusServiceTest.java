package umc.cockple.demo.domain.chat.service.websocket.send.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentMessageReadStatusService")
class SentMessageReadStatusServiceTest {

    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private SentMessageReadStatusService sentMessageReadStatusService;

    @BeforeEach
    void setUp() {
        sentMessageReadStatusService =
                new SentMessageReadStatusService(messageReadStatusRepository, chatRoomMemberRepository);
    }

    @Test
    @DisplayName("활성 구독자 중 발신자를 제외하고 읽음 처리와 lastReadMessageId 갱신을 배치 처리한다")
    void markActiveSubscribersAsRead_excludesSenderAndUpdatesReadersInBatch() {
        // given
        Long chatRoomId = 10L;
        Long messageId = 100L;
        Long senderId = 1L;
        List<Long> activeSubscribers = List.of(senderId, 2L, 3L);
        List<Long> readers = List.of(2L, 3L);

        given(messageReadStatusRepository.markAsReadInMembers(messageId, readers)).willReturn(2);
        given(chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, readers, messageId))
                .willReturn(2);
        given(messageReadStatusRepository.countUnreadByMessageId(messageId)).willReturn(1);

        // when
        int unreadCount = sentMessageReadStatusService.markActiveSubscribersAsRead(
                chatRoomId, messageId, activeSubscribers, senderId);

        // then
        assertThat(unreadCount).isEqualTo(1);
        then(messageReadStatusRepository).should().markAsReadInMembers(messageId, readers);
        then(chatRoomMemberRepository).should().advanceLastReadMessageIdForMembers(chatRoomId, readers, messageId);
    }

    @Test
    @DisplayName("시스템 메시지는 활성 구독자 전체를 읽음 처리와 lastReadMessageId 갱신 대상으로 삼는다")
    void markActiveSubscribersAsRead_includesAllSubscribersForSystemMessage() {
        // given
        Long chatRoomId = 10L;
        Long messageId = 100L;
        List<Long> activeSubscribers = List.of(1L, 2L);

        given(messageReadStatusRepository.markAsReadInMembers(messageId, activeSubscribers)).willReturn(2);
        given(chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, activeSubscribers, messageId))
                .willReturn(2);
        given(messageReadStatusRepository.countUnreadByMessageId(messageId)).willReturn(0);

        // when
        int unreadCount = sentMessageReadStatusService.markActiveSubscribersAsRead(
                chatRoomId, messageId, activeSubscribers, null);

        // then
        assertThat(unreadCount).isZero();
        then(messageReadStatusRepository).should().markAsReadInMembers(messageId, activeSubscribers);
        then(chatRoomMemberRepository).should()
                .advanceLastReadMessageIdForMembers(chatRoomId, activeSubscribers, messageId);
    }

    @Test
    @DisplayName("발신자만 활성 구독 중이면 읽음 처리와 lastReadMessageId 갱신 없이 최종 안읽음 수만 조회한다")
    void markActiveSubscribersAsRead_skipsReadUpdatesWhenOnlySenderIsActive() {
        // given
        Long chatRoomId = 10L;
        Long messageId = 100L;
        Long senderId = 1L;
        given(messageReadStatusRepository.countUnreadByMessageId(messageId)).willReturn(2);

        // when
        int unreadCount = sentMessageReadStatusService.markActiveSubscribersAsRead(
                chatRoomId, messageId, List.of(senderId), senderId);

        // then
        assertThat(unreadCount).isEqualTo(2);
        then(messageReadStatusRepository).should(never()).markAsReadInMembers(anyLong(), anyList());
        then(chatRoomMemberRepository).should(never())
                .advanceLastReadMessageIdForMembers(anyLong(), anyList(), anyLong());
    }
}
