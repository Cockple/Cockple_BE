package umc.cockple.demo.domain.chat.events;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatListSubscriptionEvent(
        Long memberId,
        List<Long> chatRoomIds,
        String action
) {
    public static ChatListSubscriptionEvent subscribe(Long memberId, List<Long> chatRoomIds) {
        return ChatListSubscriptionEvent.builder()
                .memberId(memberId)
                .chatRoomIds(chatRoomIds)
                .action("SUBSCRIBE")
                .build();
    }

    public static ChatListSubscriptionEvent unsubscribe(Long memberId, List<Long> chatRoomIds) {
        return ChatListSubscriptionEvent.builder()
                .memberId(memberId)
                .chatRoomIds(chatRoomIds)
                .action("UNSUBSCRIBE")
                .build();
    }
}
