package umc.cockple.demo.domain.chat.service.websocket.session;

import java.util.Optional;

public interface ChatMessageSender {

    boolean send(Long memberId, Object message);

    Optional<String> serialize(Object message);

    boolean sendSerialized(Long memberId, String messageJson);
}
