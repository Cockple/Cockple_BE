package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageFanout;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomMessageBroadcaster {

    private final RealtimeMessageEncoder messageEncoder;
    private final ChatMessageFanout messageFanout;

    public void broadcast(
            Long chatRoomId,
            WebSocketMessageDTO.MessageResponse message,
            List<Long> subscribers,
            Long excludedMemberId) {
        if (subscribers == null || subscribers.isEmpty()) {
            log.info("채팅방 {}에 구독 중인 사용자가 없습니다.", chatRoomId);
            return;
        }

        EncodedRealtimeMessage encodedMessage = messageEncoder.encode(message).orElse(null);
        List<Long> successMembers = new ArrayList<>();
        List<Long> failedMembers = new ArrayList<>();

        for (Long memberId : subscribers) {
            if (memberId.equals(excludedMemberId)) {
                continue;
            }

            if (messageFanout.send(memberId, encodedMessage, message.type(), message)) {
                successMembers.add(memberId);
            } else {
                failedMembers.add(memberId);
            }
        }

        log.info("브로드캐스트 완료 - 채팅방: {}, 성공: {}명, 실패: {}명", chatRoomId, successMembers.size(), failedMembers.size());
    }
}
