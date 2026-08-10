package umc.cockple.demo.global.realtime.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.realtime.logging.WebSocketMdcSupport;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open((Long) null)) {
            log.debug("JWT 기반 WebSocket 인증 시작");

            try {
                String token = extractTokenFromRequest(request);
                if (token == null || token.isBlank()) {
                    return reject(request, response, AuthenticationFailure.TOKEN_MISSING);
                }

                if (!jwtTokenProvider.validateToken(token)) {
                    return reject(request, response, AuthenticationFailure.TOKEN_INVALID);
                }

                try {
                    if (!jwtTokenProvider.isAccessToken(token)) {
                        return reject(request, response, AuthenticationFailure.INVALID_TOKEN_TYPE);
                    }
                } catch (RuntimeException e) {
                    return reject(request, response, AuthenticationFailure.TOKEN_INVALID);
                }

                Long memberId;
                try {
                    memberId = jwtTokenProvider.getUserId(token);
                } catch (NumberFormatException e) {
                    return reject(request, response, AuthenticationFailure.INVALID_SUBJECT);
                } catch (RuntimeException e) {
                    return reject(request, response, AuthenticationFailure.TOKEN_INVALID);
                }

                if (memberId == null) {
                    return reject(request, response, AuthenticationFailure.INVALID_SUBJECT);
                }

                attributes.put(WebSocketSessionAttributes.MEMBER_ID, memberId);
                attributes.put(WebSocketSessionAttributes.AUTHENTICATED, true);

                try (WebSocketMdcSupport.MdcScope memberScope = WebSocketMdcSupport.open(memberId)) {
                    log.info("JWT 인증 성공 - memberId: {}", memberId);
                }
                return true;
            } catch (Exception e) {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                log.error(
                        "WebSocket handshake 인증 처리 실패 - path: {}, reason: {}",
                        requestPath(request), AuthenticationFailure.AUTH_INTERNAL_ERROR, e
                );
                return false;
            }
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open((Long) null)) {
            if (exception != null) {
                log.error("JWT HandshakeInterceptor 실행 중 오류", exception);
            }
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

    private boolean reject(
            ServerHttpRequest request,
            ServerHttpResponse response,
            AuthenticationFailure failure
    ) {
        response.setStatusCode(failure.status);
        log.warn(
                "WebSocket handshake 인증 실패 - path: {}, reason: {}",
                requestPath(request), failure
        );
        return false;
    }

    private String requestPath(ServerHttpRequest request) {
        try {
            return request.getURI().getPath();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private enum AuthenticationFailure {
        TOKEN_MISSING(HttpStatus.UNAUTHORIZED),
        TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
        INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED),
        INVALID_SUBJECT(HttpStatus.UNAUTHORIZED),
        AUTH_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

        private final HttpStatus status;

        AuthenticationFailure(HttpStatus status) {
            this.status = status;
        }
    }
}
