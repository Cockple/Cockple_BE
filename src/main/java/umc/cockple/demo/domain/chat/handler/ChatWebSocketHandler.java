package umc.cockple.demo.domain.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.events.ChatSystemMessageEvent;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.service.ChatWebSocketService;
import umc.cockple.demo.domain.chat.service.WebSocketBroadcastService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatWebSocketService chatWebSocketService;
    private final WebSocketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("웹소켓 연결 성공");

        try {
            Long memberId = extractMemberIdFromURL(session);

            if (memberId != null) {
                session.getAttributes().put("memberId", memberId);
                broadcastService.addSessionToChatRoom(memberId, session);
                log.info("사용자 연결 완료 - memberId: {}, 세션 ID: {}", memberId, session.getId());

                sendConnectionSuccessMessage(session, memberId);
            } else {
                log.warn("memberId를 찾을 수 없습니다. 세션을 종료합니다.");
                session.close();
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생", e);
            session.close();
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("메시지 수신");
        log.info("메시지: {}", message.getPayload());

        try {
            WebSocketMessageDTO.Request request = objectMapper.readValue(
                    message.getPayload(), WebSocketMessageDTO.Request.class
            );

            Long memberId = (Long) session.getAttributes().get("memberId");
            if (memberId == null) {
                sendErrorMessage(session, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
                return;
            }

            log.info("메시지 타입: {}, 채팅방 ID: {}, 사용자 ID: {}", memberId, session.getId(), memberId);

            switch (request.type()) {
                case SEND:
                    handleSendMessage(session, request, memberId);
                    break;
                default:
                    sendErrorMessage(session, "UNKNOWN_TYPE", "알 수 없는 메시지 타입입니다:" + request.type());
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생", e);
            sendErrorMessage(session, "PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다:" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");

        log.info("웹소켓 연결 종료");
        log.info("세션 ID: {}, 사용자 ID: {}, 종료 상태: {}", session.getId(), memberId, status);

        if (memberId != null) {
            broadcastService.removeSession(memberId);
            log.info("사용자 세션 정리 완료 - memberId: {}", memberId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");
        log.error("WebSocket 전송 오류 발생 - 세션 ID: {}, 사용자 ID: {}", session.getId(), memberId, exception);
    }

    // ========== 내부 메서드들 ==========

    private Long extractMemberIdFromURL(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("memberId=")) {
            try {
                String memberIdStr = query.split("memberId=")[1].split("&")[0]; // Id값 뽑아내기.
                return Long.parseLong(memberIdStr);
            } catch (Exception e) {
                log.error("memberId 추출 실패", e);
            }
        }
        return null;
    }

    private void sendConnectionSuccessMessage(WebSocketSession session, Long memberId) {
        try {
            WebSocketMessageDTO.ConnectionInfo connectionInfo = WebSocketMessageDTO.ConnectionInfo.builder()
                    .type(WebSocketMessageType.CONNECT)
                    .memberId(memberId)
                    .connectedAt(LocalDateTime.now())
                    .message("WebSocket 연결이 성공했습니다.")
                    .build();

            String messageJson = objectMapper.writeValueAsString(connectionInfo);
            session.sendMessage(new TextMessage(messageJson));

            log.info("연결 성공 메시지 전송 완료 - memberId: {}", memberId);
        } catch (Exception e) {
            log.error("연결 성공 메시지 전송 실패", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorCode, String message) {
        if (!session.isOpen()) {
            log.warn("세션이 닫혀있어 에러 메시지를 전송할 수 없습니다.");
            return;
        }

        try {
            WebSocketMessageDTO.ErrorResponse errorResponse = WebSocketMessageDTO.ErrorResponse.builder()
                    .type(WebSocketMessageType.ERROR)
                    .errorCode(errorCode)
                    .message(message)
                    .build();

            String errorJson = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(errorJson));

            log.info("에러 메시지 전송 완료 - 코드: {}, 메시지: {}", errorCode, message);
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패", e);
        }
    }

    // ========== 메시지 처리 메서드들 ==========

    private void handleSendMessage(WebSocketSession session, WebSocketMessageDTO.Request request, Long memberId) {
        log.info("메시지 전송 처리 - 채팅방 ID: {}, 사용자 ID: {}, 내용: {}",
                request.chatRoomId(), memberId, request.content());

        try {
            WebSocketMessageDTO.Response response
                    = chatWebSocketService.sendMessage(request.chatRoomId(), request.content(), memberId);

            broadcastToChatRoom(request.chatRoomId(), response);
        } catch (ChatException e) {
            log.error("메시지 전송 중 오류 발생", e);
            sendErrorMessage(session, e.getCode().toString(), e.getMessage());
        } catch (Exception e) {
            log.error("메시지 전송 중 예상치 못한 오류 발생", e);
            sendErrorMessage(session, "INTERNAL_ERROR", "예상치 못한 에러가 발생했습니다.");
        }
    }

    @EventListener
    @Async
    public void handleSystemMessage(ChatSystemMessageEvent event) {
        log.info("시스템 메시지 브로드캐스트 시작 - 채팅방: {}", event.chatRoomId());

        try {
            WebSocketMessageDTO.Response response = WebSocketMessageDTO.Response.builder()
                    .type(WebSocketMessageType.SEND)
                    .chatRoomId(event.chatRoomId())
                    .senderId(null)
                    .senderName("시스템")
                    .senderProfileImageUrl(null)
                    .content(event.content())
                    .createdAt(event.occurredAt())
                    .build();

            broadcastToChatRoom(event.chatRoomId(), response);

        } catch (Exception e) {
            log.error("시스템 메시지 브로드캐스트 실패 - 채팅방: {}", event.chatRoomId(), e);
        }
    }

    private void broadcastToChatRoom(Long chatRoomId, WebSocketMessageDTO.Response message) {
        Map<Long, WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions == null || sessions.isEmpty()) {
            log.info("채팅방 {}에 구독 중인 세션이 없습니다.", chatRoomId);
            return;
        }

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("메시지를 JSON으로 변환하는데 실패했습니다", e);
            return;
        }

        Map<Long, WebSocketSession> sessionsCopy = new HashMap<>(sessions);
        List<Long> failedSessions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        sessionsCopy.entrySet().parallelStream().forEach(entry -> {
            Long memberId = entry.getKey();
            WebSocketSession session = entry.getValue();

            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(messageJson));
                    }
                    successCount.incrementAndGet();
                    log.debug("메시지 전송 성공 - 사용자: {}, 세션: {}", memberId, session.getId());
                } catch (Exception e) {
                    log.error("메시지 전송 실패 - 사용자: {}, 세션: {}", memberId, session.getId(), e);
                    failedSessions.add(memberId);
                }
            } else {
                log.warn("닫힌 세션 발견 - 사용자: {}, 세션: {}", memberId, session.getId());
                failedSessions.add(memberId);
            }
        });

        cleanupFailedSessions(chatRoomId, failedSessions);

        log.info("브로드캐스트 완료 - 채팅방: {}, 성공: {}명, 실패: {}명", chatRoomId, successCount, failedSessions.size());
    }

    private void cleanupFailedSessions(Long chatRoomId, List<Long> failedSessionIds) {
        if (failedSessionIds.isEmpty()) return;

        Map<Long, WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions != null) {
            failedSessionIds.forEach(sessions::remove);

            if (sessions.isEmpty()) {
                chatRoomSessions.remove(chatRoomId);
                log.info("빈 채팅방 세션 맵 제거 - 채팅방: {}", chatRoomId);
            }
        }
    }

    // ========== 테스트용 ==========
    void clearAllSessionsForTest() {
        memberSessions.clear();
        chatRoomSessions.clear();
    }

    void addChatRoomSessionForTest(Long chatRoomId, Long memberId, WebSocketSession session) {
        chatRoomSessions.computeIfAbsent(chatRoomId, k -> new ConcurrentHashMap<>())
                .put(memberId, session);
    }

    void broadcastToChatRoomForTest(Long chatRoomId, WebSocketMessageDTO.Response message) {
        broadcastToChatRoom(chatRoomId, message);
    }

    boolean isMemberInChatRoomForTest(Long chatRoomId, Long memberId) {
        Map<Long, WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        return sessions != null && sessions.containsKey(memberId);
    }
}
