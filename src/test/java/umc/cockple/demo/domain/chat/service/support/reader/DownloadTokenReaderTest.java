package umc.cockple.demo.domain.chat.service.support.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.domain.DownloadToken;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.DownloadTokenRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DownloadTokenReader")
class DownloadTokenReaderTest {

    @Mock
    private DownloadTokenRepository downloadTokenRepository;
    @Mock
    private DownloadToken downloadToken;

    private DownloadTokenReader downloadTokenReader;

    @BeforeEach
    void setUp() {
        downloadTokenReader = new DownloadTokenReader(downloadTokenRepository);
    }

    @Test
    @DisplayName("토큰 값으로 다운로드 토큰을 조회한다")
    void read_returnsDownloadToken() {
        String tokenValue = "valid-token";
        given(downloadTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(downloadToken));

        DownloadToken result = downloadTokenReader.read(tokenValue);

        assertThat(result).isSameAs(downloadToken);
    }

    @Test
    @DisplayName("토큰 조회 결과가 없으면 INVALID_DOWNLOAD_TOKEN 예외를 던진다")
    void read_throwsWhenDownloadTokenNotFound() {
        String tokenValue = "invalid-token";
        given(downloadTokenRepository.findByToken(tokenValue)).willReturn(Optional.empty());

        assertThatThrownBy(() -> downloadTokenReader.read(tokenValue))
                .isInstanceOfSatisfying(ChatException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ChatErrorCode.INVALID_DOWNLOAD_TOKEN));
    }
}
