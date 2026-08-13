package umc.cockple.demo.global.realtime.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JacksonRealtimeMessageEncoder implements RealtimeMessageEncoder {

    private final ObjectMapper objectMapper;

    @Override
    public Optional<EncodedRealtimeMessage> encode(Object message) {
        try {
            return Optional.of(new EncodedRealtimeMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            log.error("실시간 메시지 JSON 변환 실패", e);
            return Optional.empty();
        }
    }
}
