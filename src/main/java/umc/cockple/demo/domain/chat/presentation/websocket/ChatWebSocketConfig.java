package umc.cockple.demo.domain.chat.presentation.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import umc.cockple.demo.global.config.WebProperties;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JWTWebSocketAuthInterceptor jwtWebSocketAuthInterceptor;
    private final WebProperties webProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(chatWebSocketHandler, "/ws/chats")
                .addInterceptors(jwtWebSocketAuthInterceptor)
                .setAllowedOrigins(webProperties.getAllowedOrigins().toArray(new String[0]))
                .withSockJS(); // 브라우저 호환성
    }
}
