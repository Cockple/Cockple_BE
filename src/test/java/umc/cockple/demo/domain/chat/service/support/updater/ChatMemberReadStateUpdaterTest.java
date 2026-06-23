package umc.cockple.demo.domain.chat.service.support.updater;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.service.support.ReadStatusBatchSupport;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemberReadStateUpdater")
class ChatMemberReadStateUpdaterTest {

    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatMemberReadStateUpdater chatMemberReadStateUpdater;

    @BeforeEach
    void setUp() {
        chatMemberReadStateUpdater = new ChatMemberReadStateUpdater(chatRoomMemberRepository);
    }

    @Test
    @DisplayName("멤버의 마지막 읽은 메시지 ID를 전진시킨다")
    void advanceLastReadMessageId_delegatesToRepository() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        Long messageId = 201L;
        given(chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, messageId))
                .willReturn(1);

        // when
        int result = chatMemberReadStateUpdater.advanceLastReadMessageId(chatRoomId, memberId, messageId);

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("멤버의 마지막 읽은 메시지 ID가 갱신되지 않으면 0을 반환한다")
    void advanceLastReadMessageId_returnsZeroWhenNotUpdated() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        Long messageId = 201L;
        given(chatRoomMemberRepository.advanceLastReadMessageId(chatRoomId, memberId, messageId))
                .willReturn(0);

        // when
        int result = chatMemberReadStateUpdater.advanceLastReadMessageId(chatRoomId, memberId, messageId);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("여러 멤버의 마지막 읽은 메시지 ID를 전진시킨다")
    void advanceLastReadMessageIdForMembers_delegatesToRepository() {
        // given
        Long chatRoomId = 10L;
        Long messageId = 201L;
        List<Long> memberIds = List.of(101L, 102L);
        given(chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId))
                .willReturn(2);

        // when
        int result = chatMemberReadStateUpdater.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId);

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 멤버의 마지막 읽은 메시지 ID가 일부만 갱신되면 갱신 수를 반환한다")
    void advanceLastReadMessageIdForMembers_returnsPartialUpdatedCount() {
        // given
        Long chatRoomId = 10L;
        Long messageId = 201L;
        List<Long> memberIds = List.of(101L, 102L);
        given(chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId))
                .willReturn(1);

        // when
        int result = chatMemberReadStateUpdater.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId);

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 멤버의 마지막 읽은 메시지 ID 갱신은 큰 멤버 ID 목록을 chunk로 나눠 처리한다")
    void advanceLastReadMessageIdForMembers_chunksLargeMemberIds() {
        // given
        Long chatRoomId = 10L;
        Long messageId = 201L;
        List<Long> memberIds = LongStream.rangeClosed(1, ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE + 1L)
                .boxed()
                .toList();
        List<Long> firstChunk = memberIds.subList(0, ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE);
        List<Long> secondChunk = memberIds.subList(ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE, memberIds.size());

        given(chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, firstChunk, messageId))
                .willReturn(ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE);
        given(chatRoomMemberRepository.advanceLastReadMessageIdForMembers(chatRoomId, secondChunk, messageId))
                .willReturn(1);

        // when
        int result = chatMemberReadStateUpdater.advanceLastReadMessageIdForMembers(chatRoomId, memberIds, messageId);

        // then
        assertThat(result).isEqualTo(memberIds.size());
        then(chatRoomMemberRepository).should()
                .advanceLastReadMessageIdForMembers(chatRoomId, firstChunk, messageId);
        then(chatRoomMemberRepository).should()
                .advanceLastReadMessageIdForMembers(chatRoomId, secondChunk, messageId);
    }

    @Test
    @DisplayName("마지막 읽은 메시지를 갱신할 멤버 목록이 비어 있으면 갱신하지 않는다")
    void advanceLastReadMessageIdForMembers_skipsRepositoryWhenMemberIdsEmpty() {
        // when
        int result = chatMemberReadStateUpdater.advanceLastReadMessageIdForMembers(10L, List.of(), 201L);

        // then
        assertThat(result).isZero();
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }
}
