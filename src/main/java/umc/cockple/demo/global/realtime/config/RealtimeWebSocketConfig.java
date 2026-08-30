package umc.cockple.demo.global.realtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import umc.cockple.demo.global.config.WebProperties;
import umc.cockple.demo.global.realtime.auth.JwtWebSocketHandshakeInterceptor;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketEndpoint;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor;
    private final WebProperties webProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(realtimeWebSocketHandler, RealtimeWebSocketEndpoint.PATH)
                .addInterceptors(jwtWebSocketHandshakeInterceptor)
                .setAllowedOrigins(webProperties.getAllowedOrigins().toArray(new String[0]))
                .withSockJS();
    }
}
