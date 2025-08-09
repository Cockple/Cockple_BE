package umc.cockple.demo.domain.chat.events;

import lombok.Builder;

@Builder
public record ChatRoomSubscriptionEvent(
        Long chatRoomId,
        Long memberId,
        String action
) {
    public static ChatRoomSubscriptionEvent subscribe(Long chatRoomId, Long memberId) {
        return ChatRoomSubscriptionEvent.builder()
                .chatRoomId(chatRoomId)
                .memberId(memberId)
                .action("SUBSCRIBE")
                .build();
    }
}
