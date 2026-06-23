package umc.cockple.demo.domain.chat.service.support.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.repository.projection.ChatMessageUnreadCountDTO;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadStatusReader")
class ReadStatusReaderTest {

    @Mock private MessageReadStatusRepository messageReadStatusRepository;

    private ReadStatusReader readStatusReader;

    @BeforeEach
    void setUp() {
        readStatusReader = new ReadStatusReader(messageReadStatusRepository);
    }

    @Test
    @DisplayName("멤버의 unread 메시지 ID 목록을 조회한다")
    void findUnreadMessageIds_returnsMessageIds() {
        // given
        Long chatRoomId = 10L;
        Long memberId = 101L;
        List<Long> messageIds = List.of(201L, 202L);
        given(messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId))
                .willReturn(messageIds);

        // when
        List<Long> result = readStatusReader.findUnreadMessageIds(chatRoomId, memberId);

        // then
        assertThat(result).isEqualTo(messageIds);
    }

    @Test
    @DisplayName("메시지별 unread 수를 Map으로 변환한다")
    void countUnreadByMessageIds_returnsCountMap() {
        // given
        Long firstMessageId = 201L;
        Long secondMessageId = 202L;
        List<Long> messageIds = List.of(firstMessageId, secondMessageId);
        given(messageReadStatusRepository.countUnreadByMessageIds(messageIds))
                .willReturn(List.of(
                        new ChatMessageUnreadCountDTO(firstMessageId, 2L),
                        new ChatMessageUnreadCountDTO(secondMessageId, 1L)
                ));

        // when
        Map<Long, Integer> result = readStatusReader.countUnreadByMessageIds(messageIds);

        // then
        assertThat(result).containsEntry(firstMessageId, 2)
                .containsEntry(secondMessageId, 1);
    }

    @Test
    @DisplayName("메시지 ID 목록이 비어 있으면 unread 수를 조회하지 않는다")
    void countUnreadByMessageIds_skipsRepositoryWhenMessageIdsEmpty() {
        // when
        Map<Long, Integer> result = readStatusReader.countUnreadByMessageIds(List.of());

        // then
        assertThat(result).isEmpty();
        then(messageReadStatusRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("단일 메시지 unread 수를 조회한다")
    void countUnreadByMessageId_returnsCount() {
        // given
        Long messageId = 201L;
        given(messageReadStatusRepository.countUnreadByMessageId(messageId)).willReturn(3);

        // when
        int result = readStatusReader.countUnreadByMessageId(messageId);

        // then
        assertThat(result).isEqualTo(3);
    }
}
