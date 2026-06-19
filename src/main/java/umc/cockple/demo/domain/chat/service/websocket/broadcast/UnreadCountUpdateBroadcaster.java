package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnreadCountUpdateBroadcaster {

    private final ChatMessageSender messageSender;

    public void broadcast(
            Long chatRoomId,
            List<SubscribeReadStatusService.MessageUnreadUpdate> updates,
            List<Long> subscribers,
            Long excludedMemberId) {
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (SubscribeReadStatusService.MessageUnreadUpdate update : updates) {
            WebSocketMessageDTO.UnreadCountUpdateMessage updateMessage = WebSocketMessageDTO.UnreadCountUpdateMessage.builder()
                    .type(WebSocketMessageType.UNREAD_COUNT_UPDATE)
                    .chatRoomId(chatRoomId)
                    .messageId(update.messageId())
                    .newUnreadCount(update.newUnreadCount())
                    .timestamp(LocalDateTime.now())
                    .build();

            String messageJson = messageSender.serialize(updateMessage).orElse(null);
            if (messageJson == null) {
                log.error("안읽은 수 업데이트 메시지 생성 실패 - 메시지: {}", update.messageId());
                continue;
            }

            int successCount = 0;
            for (Long memberId : subscribers) {
                if (memberId.equals(excludedMemberId)) {
                    continue;
                }

                if (messageSender.sendSerialized(memberId, messageJson)) {
                    successCount++;
                } else {
                    log.error("안읽은 수 업데이트 브로드캐스트 실패 - 사용자: {}, 메시지: {}",
                            memberId, update.messageId());
                }
            }

            log.debug("메시지 {} 안읽은 수 업데이트 브로드캐스트 완료 - 성공: {}명, 새 안읽은 수: {}",
                    update.messageId(), successCount, update.newUnreadCount());
        }
    }
}
