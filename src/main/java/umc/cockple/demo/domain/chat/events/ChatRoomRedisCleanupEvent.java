package umc.cockple.demo.domain.chat.events;

public record ChatRoomRedisCleanupEvent(
        Long chatRoomId
) {
    public static ChatRoomRedisCleanupEvent of(Long chatRoomId) {
        return new ChatRoomRedisCleanupEvent(chatRoomId);
    }
}
