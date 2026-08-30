package umc.cockple.demo.domain.chat.service.support.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatFileReader")
class ChatFileReaderTest {

    @Mock
    private ChatFileRepository chatFileRepository;
    @Mock
    private ChatMessageFile chatFile;

    private ChatFileReader chatFileReader;

    @BeforeEach
    void setUp() {
        chatFileReader = new ChatFileReader(chatFileRepository);
    }

    @Test
    @DisplayName("파일 ID로 채팅 파일을 조회한다")
    void read_returnsChatFile() {
        Long fileId = 1L;
        given(chatFileRepository.findById(fileId)).willReturn(Optional.of(chatFile));

        ChatMessageFile result = chatFileReader.read(fileId);

        assertThat(result).isSameAs(chatFile);
    }

    @Test
    @DisplayName("파일 ID 조회 결과가 없으면 FILE_NOT_FOUND 예외를 던진다")
    void read_throwsWhenChatFileNotFound() {
        Long fileId = 1L;
        given(chatFileRepository.findById(fileId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatFileReader.read(fileId))
                .isInstanceOfSatisfying(ChatException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ChatErrorCode.FILE_NOT_FOUND));
    }
}
