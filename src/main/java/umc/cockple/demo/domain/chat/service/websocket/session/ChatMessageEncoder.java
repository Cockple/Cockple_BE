package umc.cockple.demo.domain.chat.service.websocket.session;

import java.util.Optional;

public interface ChatMessageEncoder {

    Optional<EncodedChatMessage> encode(Object message);
}
