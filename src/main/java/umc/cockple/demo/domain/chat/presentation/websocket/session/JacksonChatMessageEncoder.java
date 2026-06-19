package umc.cockple.demo.domain.chat.presentation.websocket.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JacksonChatMessageEncoder implements ChatMessageEncoder {

    private final ObjectMapper objectMapper;

    @Override
    public Optional<EncodedChatMessage> encode(Object message) {
        try {
            return Optional.of(new EncodedChatMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            log.error("WebSocket 메시지 JSON 변환 실패", e);
            return Optional.empty();
        }
    }
}
