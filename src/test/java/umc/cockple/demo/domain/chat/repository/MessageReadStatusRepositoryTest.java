package umc.cockple.demo.domain.chat.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;
import umc.cockple.demo.domain.chat.repository.projection.ChatMessageUnreadCountDTO;
import umc.cockple.demo.global.config.QuerydslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageReadStatusRepository")
@DataJpaTest
@Import(QuerydslConfig.class)
class MessageReadStatusRepositoryTest {

    @Autowired private MessageReadStatusRepository messageReadStatusRepository;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("markMessagesAsReadForMember는 지정 채팅방/멤버/메시지 목록의 unread 상태만 읽음 처리한다")
    void markMessagesAsReadForMember_updatesOnlyMatchingUnreadStatuses() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        Long otherMemberId = 102L;
        Long otherChatRoomId = 11L;
        Long firstMessageId = 201L;
        Long secondMessageId = 202L;
        Long excludedMessageId = 203L;
        Long otherRoomMessageId = 204L;

        MessageReadStatus firstUnread = saveUnread(firstMessageId, memberId, chatRoomId);
        MessageReadStatus secondUnread = saveUnread(secondMessageId, memberId, chatRoomId);
        MessageReadStatus excludedUnread = saveUnread(excludedMessageId, memberId, chatRoomId);
        MessageReadStatus otherMemberUnread = saveUnread(firstMessageId, otherMemberId, chatRoomId);
        MessageReadStatus otherRoomUnread = saveUnread(otherRoomMessageId, memberId, otherChatRoomId);
        MessageReadStatus alreadyRead = saveRead(205L, memberId, chatRoomId);

        // when
        int updatedCount = messageReadStatusRepository.markMessagesAsReadForMember(
                chatRoomId,
                memberId,
                List.of(firstMessageId, secondMessageId, otherRoomMessageId)
        );

        // then
        assertThat(updatedCount).isEqualTo(2);
        entityManager.clear();

        assertThat(readStatus(firstUnread).getIsRead()).isTrue();
        assertThat(readStatus(secondUnread).getIsRead()).isTrue();
        assertThat(readStatus(excludedUnread).getIsRead()).isFalse();
        assertThat(readStatus(otherMemberUnread).getIsRead()).isFalse();
        assertThat(readStatus(otherRoomUnread).getIsRead()).isFalse();
        assertThat(readStatus(alreadyRead).getIsRead()).isTrue();
    }

    @Test
    @DisplayName("countUnreadByMessageIds는 메시지별 unread 수를 그룹으로 조회하고 fully-read 메시지는 결과에서 제외한다")
    void countUnreadByMessageIds_countsUnreadByMessageAndOmitsFullyReadMessages() {
        // given
        Long chatRoomId = 10L;
        Long firstMessageId = 201L;
        Long secondMessageId = 202L;
        Long fullyReadMessageId = 203L;
        Long excludedMessageId = 204L;

        saveUnread(firstMessageId, 101L, chatRoomId);
        saveUnread(firstMessageId, 102L, chatRoomId);
        saveRead(firstMessageId, 103L, chatRoomId);
        saveUnread(secondMessageId, 101L, chatRoomId);
        saveRead(fullyReadMessageId, 101L, chatRoomId);
        saveRead(fullyReadMessageId, 102L, chatRoomId);
        saveUnread(excludedMessageId, 101L, chatRoomId);

        // when
        List<ChatMessageUnreadCountDTO> result = messageReadStatusRepository.countUnreadByMessageIds(
                List.of(firstMessageId, secondMessageId, fullyReadMessageId)
        );

        // then
        assertThat(result)
                .extracting(ChatMessageUnreadCountDTO::chatMessageId)
                .containsExactlyInAnyOrder(firstMessageId, secondMessageId);
        assertThat(result)
                .anySatisfy(count -> {
                    assertThat(count.chatMessageId()).isEqualTo(firstMessageId);
                    assertThat(count.unreadCount()).isEqualTo(2L);
                })
                .anySatisfy(count -> {
                    assertThat(count.chatMessageId()).isEqualTo(secondMessageId);
                    assertThat(count.unreadCount()).isEqualTo(1L);
                })
                .noneSatisfy(count -> assertThat(count.chatMessageId()).isEqualTo(fullyReadMessageId))
                .noneSatisfy(count -> assertThat(count.chatMessageId()).isEqualTo(excludedMessageId));
    }

    private MessageReadStatus saveUnread(Long messageId, Long memberId, Long chatRoomId) {
        return messageReadStatusRepository.save(MessageReadStatus.createUnread(messageId, memberId, chatRoomId));
    }

    private MessageReadStatus saveRead(Long messageId, Long memberId, Long chatRoomId) {
        return messageReadStatusRepository.save(MessageReadStatus.createRead(messageId, memberId, chatRoomId));
    }

    private MessageReadStatus readStatus(MessageReadStatus status) {
        return messageReadStatusRepository.findById(status.getId()).orElseThrow();
    }
}
