package umc.cockple.demo.domain.chat.service.websocket.session;

public interface ChatMessageSender {

    boolean send(Long memberId, EncodedChatMessage message);
}
