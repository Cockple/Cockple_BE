package umc.cockple.demo.domain.chat.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatFileReader {

    private final ChatFileRepository chatFileRepository;

    public ChatMessageFile read(Long fileId) {
        return chatFileRepository.findById(fileId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.FILE_NOT_FOUND));
    }
}
