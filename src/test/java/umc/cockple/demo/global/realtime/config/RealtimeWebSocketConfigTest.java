package umc.cockple.demo.global.realtime.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import umc.cockple.demo.global.config.WebProperties;
import umc.cockple.demo.global.realtime.auth.JwtWebSocketHandshakeInterceptor;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketEndpoint;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketHandler;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("RealtimeWebSocketConfig")
class RealtimeWebSocketConfigTest {

    @Test
    @DisplayName("공용 handler를 /ws/realtime에 JWT 인증, 허용 origin, SockJS와 함께 등록한다")
    void registersRealtimeEndpoint() {
        RealtimeWebSocketHandler handler = mock(RealtimeWebSocketHandler.class);
        JwtWebSocketHandshakeInterceptor authInterceptor = mock(JwtWebSocketHandshakeInterceptor.class);
        WebProperties webProperties = new WebProperties();
        webProperties.setAllowedOrigins(List.of("https://cockple.site", "http://localhost:5173"));
        RealtimeWebSocketConfig config = new RealtimeWebSocketConfig(
                handler,
                authInterceptor,
                webProperties
        );
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        given(registry.addHandler(handler, RealtimeWebSocketEndpoint.PATH)).willReturn(registration);
        given(registration.addInterceptors(authInterceptor)).willReturn(registration);
        given(registration.setAllowedOrigins("https://cockple.site", "http://localhost:5173"))
                .willReturn(registration);

        config.registerWebSocketHandlers(registry);

        then(registry).should().addHandler(handler, "/ws/realtime");
        then(registration).should().addInterceptors(authInterceptor);
        then(registration).should()
                .setAllowedOrigins("https://cockple.site", "http://localhost:5173");
        then(registration).should().withSockJS();
    }
}
