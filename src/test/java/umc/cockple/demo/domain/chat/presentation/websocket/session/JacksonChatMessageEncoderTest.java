package umc.cockple.demo.domain.chat.presentation.websocket.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonChatMessageEncoder")
class JacksonChatMessageEncoderTest {

    private final JacksonChatMessageEncoder messageEncoder =
            new JacksonChatMessageEncoder(new ObjectMapper().findAndRegisterModules());

    @Test
    @DisplayName("WebSocket 메시지를 전송 가능한 payload로 인코딩한다")
    void encode_serializesMessagePayload() {
        // given
        WebSocketMessageDTO.UnreadStatusUpdateMessage message =
                WebSocketMessageDTO.UnreadStatusUpdateMessage.builder()
                        .type(WebSocketMessageType.UNREAD_STATUS_UPDATE)
                        .hasUnread(true)
                        .hasPartyUnread(true)
                        .hasDirectUnread(false)
                        .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                        .build();

        // when
        Optional<EncodedChatMessage> encodedMessage = messageEncoder.encode(message);

        // then
        assertThat(encodedMessage).isPresent();
        assertThat(encodedMessage.get().payload()).contains("\"type\":\"UNREAD_STATUS_UPDATE\"");
    }
}
