package umc.cockple.demo.domain.chat.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatListSubscriptionService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CHAT_LIST_SUBSCRIBERS = "chatlist:subscribers:";

    public void subscribeToChatList(Long memberId, List<Long> chatRoomIds) {
        try {
            for (Long chatRoomId : chatRoomIds) {
                String key = CHAT_LIST_SUBSCRIBERS + chatRoomId;
                stringRedisTemplate.opsForSet().add(key, memberId.toString());
                stringRedisTemplate.expire(key, Duration.ofHours(2));
            }
            log.info("채팅방 목록 구독 완료 - 멤버: {}, 채팅방 수: {}", memberId, chatRoomIds.size());
        } catch (Exception e) {
            log.error("채팅방 목록 구독 실패 - 멤버: {}", memberId, e);
        }
    }

    public void unsubscribeFromChatList(Long memberId, List<Long> chatRoomIds) {
        try {
            for (Long chatRoomId : chatRoomIds) {
                String key = CHAT_LIST_SUBSCRIBERS + chatRoomId;
                stringRedisTemplate.opsForSet().remove(key, memberId.toString());
            }
            log.info("채팅방 목록 구독 해제 완료 - 멤버: {}", memberId);
        } catch (Exception e) {
            log.error("채팅방 목록 구독 해제 실패 - 멤버: {}", memberId, e);
        }
    }

    public Set<Long> getChatListSubscribers(Long chatRoomId) {
        try {
            String key = CHAT_LIST_SUBSCRIBERS + chatRoomId;
            Set<String> members = stringRedisTemplate.opsForSet().members(key);

            if (members == null || members.isEmpty()) {
                return Set.of();
            }

            return members.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("채팅방 목록 구독자 조회 실패 - 채팅방: {}", chatRoomId, e);
            return Set.of();
        }
    }
}
