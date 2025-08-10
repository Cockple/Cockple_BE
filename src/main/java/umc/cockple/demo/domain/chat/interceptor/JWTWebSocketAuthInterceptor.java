package umc.cockple.demo.domain.chat.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JWTWebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("JWT 기반 WebSocket 인증 시작");

        try {
            String token = extractTokenFromRequest(request);

            if (isValidToken(token)) return false;

            Long memberId = jwtTokenProvider.getUserId(token);

            attributes.put("memberId", memberId);
            attributes.put("authenticated", true);

            log.info("JWT 인증 성공 - memberId: {}", memberId);
            return true;
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("JWT HandshakeInterceptor 실행 중 오류", exception);
        }
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        String query = request.getURI().getQuery();

        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }

        return null;
    }

    private boolean isValidToken(String token) {
        if (token == null) {
            log.error("JWT 토큰이 없습니다");
            return true;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.error("유효하지 않은 JWT 토큰");
            return true;
        }
        return false;
    }
}
