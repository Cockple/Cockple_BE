package umc.cockple.demo.domain.chat.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.dto.LastMessageCacheDTO;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomListCacheService {

    private final ChatMessageRepository chatMessageRepository;

    @Cacheable(value = "chatRoomLastMessage", key = "#chatRoomId")
    public LastMessageCacheDTO getLastMessage(Long chatRoomId) {
        log.info("캐시 미스 - 채팅방 {} 마지막 메시지 DB 조회", chatRoomId);

        ChatMessage lastMessage = chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(chatRoomId);

        if (lastMessage == null) {
            return null;
        }

        return LastMessageCacheDTO.builder()
                .content(lastMessage.getContent())
                .timestamp(lastMessage.getCreatedAt())
                .messageType(lastMessage.getType().name())
                .build();
    }

    @CacheEvict(value = "chatRoomLastMessage", key = "#chatRoomId")
    public void evictLastMessage(Long chatRoomId) {
        log.info("채팅방 {} 마지막 메시지 캐시 무효화", chatRoomId);
    }

    @CacheEvict(value = "chatRoomLastMessage", allEntries = true)
    public void evictAllLastMessages() {
        log.info("모든 채팅방 마지막 메시지 캐시 무효화");
    }
}
