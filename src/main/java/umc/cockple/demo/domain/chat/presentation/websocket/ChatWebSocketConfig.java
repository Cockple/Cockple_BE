package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import umc.cockple.demo.global.config.WebProperties;
import umc.cockple.demo.global.realtime.auth.JwtWebSocketHandshakeInterceptor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor;
    private final WebProperties webProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(chatWebSocketHandler, "/ws/chats")
                .addInterceptors(jwtWebSocketHandshakeInterceptor)
                .setAllowedOrigins(webProperties.getAllowedOrigins().toArray(new String[0]))
                .withSockJS(); // 브라우저 호환성
    }
}
