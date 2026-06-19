package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomMessageBroadcaster {

    private final ChatMessageSender messageSender;

    public void broadcast(
            Long chatRoomId,
            WebSocketMessageDTO.MessageResponse message,
            List<Long> subscribers,
            Long excludedMemberId) {
        if (subscribers == null || subscribers.isEmpty()) {
            log.info("채팅방 {}에 구독 중인 사용자가 없습니다.", chatRoomId);
            return;
        }

        String messageJson = messageSender.serialize(message).orElse(null);
        if (messageJson == null) {
            return;
        }

        List<Long> successMembers = new ArrayList<>();
        List<Long> failedMembers = new ArrayList<>();

        for (Long memberId : subscribers) {
            if (memberId.equals(excludedMemberId)) {
                continue;
            }

            if (messageSender.sendSerialized(memberId, messageJson)) {
                successMembers.add(memberId);
            } else {
                failedMembers.add(memberId);
            }
        }

        log.info("브로드캐스트 완료 - 채팅방: {}, 성공: {}명, 실패: {}명", chatRoomId, successMembers.size(), failedMembers.size());
    }
}
