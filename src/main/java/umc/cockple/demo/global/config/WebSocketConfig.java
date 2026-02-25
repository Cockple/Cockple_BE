package umc.cockple.demo.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import umc.cockple.demo.domain.chat.handler.ChatWebSocketHandler;
import umc.cockple.demo.domain.chat.interceptor.JWTWebSocketAuthInterceptor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JWTWebSocketAuthInterceptor jwtWebSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(chatWebSocketHandler, "/ws/chats")
                .addInterceptors(jwtWebSocketAuthInterceptor)
                .setAllowedOrigins("http://localhost:5173", "https://cockple.store", "https://cockple-fe.vercel.app/", "https://staging.cockple.store")
                .withSockJS(); // 브라우저 호환성
    }
}
