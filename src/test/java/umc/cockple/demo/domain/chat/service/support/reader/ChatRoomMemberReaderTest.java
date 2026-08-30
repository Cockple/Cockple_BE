package umc.cockple.demo.domain.chat.service.support.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomMemberReader")
class ChatRoomMemberReaderTest {

    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatRoomMemberReader chatRoomMemberReader;

    @BeforeEach
    void setUp() {
        chatRoomMemberReader = new ChatRoomMemberReader(chatRoomMemberRepository);
    }

    @Test
    @DisplayName("채팅방 멤버 존재 여부를 반환한다")
    void exists_returnsRepositoryResult() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 2L;
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId))
                .willReturn(true);

        // when
        boolean result = chatRoomMemberReader.exists(chatRoomId, memberId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("채팅방 멤버가 없으면 false를 반환한다")
    void exists_returnsFalseWhenChatRoomMemberNotFound() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 2L;
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId))
                .willReturn(false);

        // when
        boolean result = chatRoomMemberReader.exists(chatRoomId, memberId);

        // then
        assertThat(result).isFalse();
    }
}
