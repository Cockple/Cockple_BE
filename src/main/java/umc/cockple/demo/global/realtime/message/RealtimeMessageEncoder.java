package umc.cockple.demo.global.realtime.message;

import java.util.Optional;

public interface RealtimeMessageEncoder {

    Optional<EncodedRealtimeMessage> encode(Object message);
}
