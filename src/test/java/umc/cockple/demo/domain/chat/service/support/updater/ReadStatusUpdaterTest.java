package umc.cockple.demo.domain.chat.service.support.updater;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.support.ReadStatusBatchSupport;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadStatusUpdater")
class ReadStatusUpdaterTest {

    @Mock private MessageReadStatusRepository messageReadStatusRepository;

    private ReadStatusUpdater readStatusUpdater;

    @BeforeEach
    void setUp() {
        readStatusUpdater = new ReadStatusUpdater(messageReadStatusRepository);
    }

    @Test
    @DisplayName("멤버의 메시지 목록을 읽음 처리한다")
    void markMessagesAsReadForMember_delegatesToRepository() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        List<Long> messageIds = List.of(201L, 202L);
        given(messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, messageIds))
                .willReturn(2);

        // when
        int result = readStatusUpdater.markMessagesAsReadForMember(chatRoomId, memberId, messageIds);

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("멤버의 메시지 목록 읽음 처리는 큰 메시지 ID 목록을 chunk로 나눠 갱신한다")
    void markMessagesAsReadForMember_chunksLargeMessageIds() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        List<Long> messageIds = LongStream.rangeClosed(1, ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE + 1L)
                .boxed()
                .toList();
        List<Long> firstChunk = messageIds.subList(0, ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE);
        List<Long> secondChunk = messageIds.subList(ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE, messageIds.size());

        given(messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, firstChunk))
                .willReturn(ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE);
        given(messageReadStatusRepository.markMessagesAsReadForMember(chatRoomId, memberId, secondChunk))
                .willReturn(1);

        // when
        int result = readStatusUpdater.markMessagesAsReadForMember(chatRoomId, memberId, messageIds);

        // then
        assertThat(result).isEqualTo(messageIds.size());
        then(messageReadStatusRepository).should()
                .markMessagesAsReadForMember(chatRoomId, memberId, firstChunk);
        then(messageReadStatusRepository).should()
                .markMessagesAsReadForMember(chatRoomId, memberId, secondChunk);
    }

    @Test
    @DisplayName("읽음 처리할 메시지 목록이 비어 있으면 갱신하지 않는다")
    void markMessagesAsReadForMember_skipsRepositoryWhenMessageIdsEmpty() {
        // when
        int result = readStatusUpdater.markMessagesAsReadForMember(10L, 101L, List.of());

        // then
        assertThat(result).isZero();
        then(messageReadStatusRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("메시지를 여러 멤버에 대해 읽음 처리한다")
    void markMessageAsReadForMembers_delegatesToRepository() {
        // given
        Long messageId = 201L;
        List<Long> memberIds = List.of(101L, 102L);
        given(messageReadStatusRepository.markAsReadInMembers(messageId, memberIds)).willReturn(2);

        // when
        int result = readStatusUpdater.markMessageAsReadForMembers(messageId, memberIds);

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 멤버 읽음 처리는 큰 멤버 ID 목록을 chunk로 나눠 갱신한다")
    void markMessageAsReadForMembers_chunksLargeMemberIds() {
        // given
        Long messageId = 201L;
        List<Long> memberIds = LongStream.rangeClosed(1, ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE + 1L)
                .boxed()
                .toList();
        List<Long> firstChunk = memberIds.subList(0, ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE);
        List<Long> secondChunk = memberIds.subList(ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE, memberIds.size());

        given(messageReadStatusRepository.markAsReadInMembers(messageId, firstChunk))
                .willReturn(ReadStatusBatchSupport.IN_CLAUSE_CHUNK_SIZE);
        given(messageReadStatusRepository.markAsReadInMembers(messageId, secondChunk))
                .willReturn(1);

        // when
        int result = readStatusUpdater.markMessageAsReadForMembers(messageId, memberIds);

        // then
        assertThat(result).isEqualTo(memberIds.size());
        then(messageReadStatusRepository).should().markAsReadInMembers(messageId, firstChunk);
        then(messageReadStatusRepository).should().markAsReadInMembers(messageId, secondChunk);
    }

    @Test
    @DisplayName("읽음 처리할 멤버 목록이 비어 있으면 갱신하지 않는다")
    void markMessageAsReadForMembers_skipsRepositoryWhenMemberIdsEmpty() {
        // when
        int result = readStatusUpdater.markMessageAsReadForMembers(201L, List.of());

        // then
        assertThat(result).isZero();
        then(messageReadStatusRepository).shouldHaveNoInteractions();
    }
}
