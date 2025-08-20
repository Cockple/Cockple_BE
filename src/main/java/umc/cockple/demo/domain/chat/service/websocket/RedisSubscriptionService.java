package umc.cockple.demo.domain.chat.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriptionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CHAT_ROOM_SUBSCRIBERS = "chatroom:subscribers:";

    private static final Duration SUBSCRIPTION_TTL = Duration.ofHours(2);

    public void addSubscriber(Long chatRoomId, Long memberId) {
        try {
            String subscribersKey = CHAT_ROOM_SUBSCRIBERS + chatRoomId;
            redisTemplate.opsForSet().add(subscribersKey, memberId);
            redisTemplate.expire(subscribersKey, SUBSCRIPTION_TTL);

            log.info("Redis 구독 추가 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        } catch (Exception e) {
            log.error("Redis 구독 추가 실패 - 채팅방: {}, 멤버: {}", chatRoomId, memberId, e);
        }
    }

    public void removeSubscriber(Long chatRoomId, Long memberId) {
        try {
            String subscribersKey = CHAT_ROOM_SUBSCRIBERS + chatRoomId;
            redisTemplate.opsForSet().remove(subscribersKey, memberId);

            Long remainingCount = redisTemplate.opsForSet().size(subscribersKey);
            if (remainingCount != null && remainingCount == 0) {
                redisTemplate.delete(subscribersKey);
                log.debug("빈 구독 키 삭제 - 채팅방: {}", chatRoomId);
            } else {
                redisTemplate.expire(subscribersKey, SUBSCRIPTION_TTL);
            }

            log.info("Redis 구독 제거 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        } catch (Exception e) {
            log.error("Redis 구독 제거 실패 - 채팅방: {}, 멤버: {}", chatRoomId, memberId, e);
        }
    }

    public Set<Long> getSubscribers(Long chatRoomId) {
        try {
            String subscribersKey = CHAT_ROOM_SUBSCRIBERS + chatRoomId;

            if (redisTemplate.hasKey(subscribersKey)) {
                redisTemplate.expire(subscribersKey, SUBSCRIPTION_TTL);
            }

            Set<Object> members = redisTemplate.opsForSet().members(subscribersKey);

            if (members == null || members.isEmpty()) {
                return Set.of();
            }

            return members.stream()
                    .map(Long.class::cast)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("Redis 구독자 조회 실패 - 채팅방: {}", chatRoomId, e);
            return Set.of();
        }
    }
}
