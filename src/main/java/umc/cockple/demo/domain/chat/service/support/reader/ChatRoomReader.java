package umc.cockple.demo.domain.chat.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomReader {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoom read(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    public ChatRoom readByPartyId(Long partyId) {
        return chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    public Optional<ChatRoom> findByPartyId(Long partyId) {
        return chatRoomRepository.findByPartyId(partyId);
    }
}
