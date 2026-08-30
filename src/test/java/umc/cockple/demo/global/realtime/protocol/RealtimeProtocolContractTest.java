package umc.cockple.demo.global.realtime.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("공용 실시간 프로토콜 계약")
class RealtimeProtocolContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("version, domain, action, requestId, payload 요청을 역직렬화한다")
    void deserializeInboundEnvelope() throws Exception {
        String json = """
                {
                  "version": 1,
                  "domain": "GAME",
                  "action": "SUBSCRIBE",
                  "requestId": "request-1",
                  "payload": {"gameId": 20}
                }
                """;

        RealtimeInboundEnvelope envelope =
                objectMapper.readValue(json, RealtimeInboundEnvelope.class);

        assertThat(envelope.version()).isEqualTo(RealtimeProtocolVersion.CURRENT);
        assertThat(envelope.domain()).isEqualTo("GAME");
        assertThat(envelope.action()).isEqualTo("SUBSCRIBE");
        assertThat(envelope.requestId()).isEqualTo("request-1");
        assertThat(envelope.payload().get("gameId").asLong()).isEqualTo(20L);
    }

    @Test
    @DisplayName("성공 응답은 data를 포함하고 error를 노출하지 않는다")
    void serializeSuccessEnvelope() {
        RealtimeOutboundEnvelope envelope = RealtimeOutboundEnvelope.success(
                "GAME",
                "ACK",
                "request-1",
                Map.of("gameId", 20L)
        );

        JsonNode json = objectMapper.valueToTree(envelope);

        assertThat(json.get("version").asInt()).isEqualTo(RealtimeProtocolVersion.CURRENT);
        assertThat(json.get("domain").asText()).isEqualTo("GAME");
        assertThat(json.get("type").asText()).isEqualTo("ACK");
        assertThat(json.get("requestId").asText()).isEqualTo("request-1");
        assertThat(json.get("data").get("gameId").asLong()).isEqualTo(20L);
        assertThat(json.has("error")).isFalse();
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }

    @Test
    @DisplayName("오류 응답은 표준 error를 포함하고 data를 노출하지 않는다")
    void serializeErrorEnvelope() {
        RealtimeOutboundEnvelope envelope = RealtimeOutboundEnvelope.error(
                "GAME",
                "request-1",
                "GAME_NOT_FOUND",
                "게임을 찾을 수 없습니다."
        );

        JsonNode json = objectMapper.valueToTree(envelope);

        assertThat(json.get("type").asText()).isEqualTo("ERROR");
        assertThat(json.get("error").get("code").asText()).isEqualTo("GAME_NOT_FOUND");
        assertThat(json.get("error").get("message").asText()).isEqualTo("게임을 찾을 수 없습니다.");
        assertThat(json.has("data")).isFalse();
    }
}
