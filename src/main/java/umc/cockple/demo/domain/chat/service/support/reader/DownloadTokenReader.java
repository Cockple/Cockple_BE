package umc.cockple.demo.domain.chat.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.DownloadToken;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.DownloadTokenRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DownloadTokenReader {

    private final DownloadTokenRepository downloadTokenRepository;

    public DownloadToken read(String tokenValue) {
        return downloadTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_DOWNLOAD_TOKEN));
    }
}
