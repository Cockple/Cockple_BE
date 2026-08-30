package umc.cockple.demo.global.realtime.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonRealtimeMessageEncoder")
class JacksonRealtimeMessageEncoderTest {

    private final JacksonRealtimeMessageEncoder messageEncoder =
            new JacksonRealtimeMessageEncoder(new ObjectMapper().findAndRegisterModules());

    @Test
    @DisplayName("실시간 메시지를 전송 가능한 payload로 인코딩한다")
    void encodeSerializesMessagePayload() {
        WebSocketMessageDTO.UnreadStatusUpdateMessage message =
                WebSocketMessageDTO.UnreadStatusUpdateMessage.builder()
                        .type(WebSocketMessageType.UNREAD_STATUS_UPDATE)
                        .hasUnread(true)
                        .hasPartyUnread(true)
                        .hasDirectUnread(false)
                        .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                        .build();

        Optional<EncodedRealtimeMessage> encodedMessage = messageEncoder.encode(message);

        assertThat(encodedMessage).isPresent();
        assertThat(encodedMessage.get().payload()).contains("\"type\":\"UNREAD_STATUS_UPDATE\"");
    }
}
