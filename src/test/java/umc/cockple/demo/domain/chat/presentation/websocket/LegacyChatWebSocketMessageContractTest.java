package umc.cockple.demo.domain.chat.presentation.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Legacy 채팅 WebSocket JSON 계약")
class LegacyChatWebSocketMessageContractTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 8, 8, 12, 30);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("기존 SEND 요청의 최상위 필드와 이미지 필드를 역직렬화한다")
    void deserializeLegacySendRequest() throws Exception {
        String payload = """
                {
                  "type": "SEND",
                  "chatRoomId": 20,
                  "memberRooms": [20, 30],
                  "content": "hello",
                  "images": [{
                    "imgKey": "chat/image.png",
                    "imgOrder": 1,
                    "originalFileName": "image.png",
                    "fileSize": 1024,
                    "fileType": "image/png"
                  }],
                  "lastReadMessageId": 100
                }
                """;

        WebSocketMessageDTO.Request request =
                objectMapper.readValue(payload, WebSocketMessageDTO.Request.class);

        assertThat(request.type()).isEqualTo(WebSocketMessageType.SEND);
        assertThat(request.chatRoomId()).isEqualTo(20L);
        assertThat(request.memberRooms()).containsExactly(20L, 30L);
        assertThat(request.content()).isEqualTo("hello");
        assertThat(request.lastReadMessageId()).isEqualTo(100L);
        assertThat(request.images()).singleElement().satisfies(image -> {
            assertThat(image.imgKey()).isEqualTo("chat/image.png");
            assertThat(image.imgOrder()).isEqualTo(1);
            assertThat(image.originalFileName()).isEqualTo("image.png");
            assertThat(image.fileSize()).isEqualTo(1024L);
            assertThat(image.fileType()).isEqualTo("image/png");
        });
    }

    @Test
    @DisplayName("기존 CONNECT 응답의 type과 필드명을 유지한다")
    void serializeLegacyConnectionResponse() throws Exception {
        WebSocketMessageDTO.ConnectionInfo response = WebSocketMessageDTO.ConnectionInfo.builder()
                .type(WebSocketMessageType.CONNECT)
                .memberId(10L)
                .memberName("홍길동")
                .timestamp(TIMESTAMP)
                .message("WebSocket 연결이 성공했습니다.")
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        assertFieldNames(json, "type", "memberId", "memberName", "timestamp", "message");
        assertThat(json.get("type").asText()).isEqualTo("CONNECT");
        assertThat(json.get("memberId").asLong()).isEqualTo(10L);
        assertThat(json.get("memberName").asText()).isEqualTo("홍길동");
        assertThat(json.get("timestamp").asText()).isEqualTo("2026-08-08T12:30:00");
    }

    @Test
    @DisplayName("기존 SEND 응답의 type과 메시지·발신자·읽음 필드를 유지한다")
    void serializeLegacyMessageResponse() {
        ChatCommonDTO.FileInfo image = ChatCommonDTO.FileInfo.builder()
                .imageId(1L)
                .imageUrl("https://cdn.example.com/image.png")
                .imgOrder(1)
                .isEmoji(false)
                .originalFileName("image.png")
                .fileSize(1024L)
                .fileType("image/png")
                .build();
        WebSocketMessageDTO.MessageResponse response = WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(20L)
                .messageId(100L)
                .content("hello")
                .messageType(MessageType.TEXT)
                .images(List.of(image))
                .senderId(10L)
                .senderName("홍길동")
                .senderProfileImageUrl("https://cdn.example.com/profile.png")
                .timestamp(TIMESTAMP)
                .unreadCount(2)
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        assertFieldNames(json,
                "type", "chatRoomId", "messageId", "content", "messageType", "images",
                "senderId", "senderName", "senderProfileImageUrl", "timestamp", "unreadCount");
        assertThat(json.get("type").asText()).isEqualTo("SEND");
        assertThat(json.get("messageType").asText()).isEqualTo("TEXT");
        assertThat(json.get("unreadCount").asInt()).isEqualTo(2);
        assertFieldNames(json.get("images").get(0),
                "imageId", "imageUrl", "imgOrder", "isEmoji",
                "originalFileName", "fileSize", "fileType");
    }

    @Test
    @DisplayName("기존 구독 ACK와 ERROR 응답의 필드명을 유지한다")
    void serializeLegacyAcknowledgementAndErrorResponses() {
        WebSocketMessageDTO.SubscriptionResponse subscription =
                WebSocketMessageDTO.SubscriptionResponse.builder()
                        .type(WebSocketMessageType.SUBSCRIBE)
                        .chatRoomId(20L)
                        .message("채팅방 구독이 완료되었습니다.")
                        .timestamp(TIMESTAMP)
                        .build();
        WebSocketMessageDTO.ChatListSubscriptionResponse chatListSubscription =
                WebSocketMessageDTO.ChatListSubscriptionResponse.builder()
                        .type(WebSocketMessageType.SUBSCRIBE_CHAT_LIST)
                        .chatRoomIds(List.of(20L, 30L))
                        .message("채팅방 목록 구독이 완료되었습니다. (총 2개)")
                        .timestamp(TIMESTAMP)
                        .build();
        WebSocketMessageDTO.ErrorResponse error = WebSocketMessageDTO.ErrorResponse.builder()
                .type(WebSocketMessageType.ERROR)
                .errorCode("CHAT4001")
                .message("채팅방에 접근할 수 없습니다.")
                .build();

        JsonNode subscriptionJson = objectMapper.valueToTree(subscription);
        JsonNode chatListJson = objectMapper.valueToTree(chatListSubscription);
        JsonNode errorJson = objectMapper.valueToTree(error);

        assertFieldNames(subscriptionJson, "type", "chatRoomId", "message", "timestamp");
        assertThat(subscriptionJson.get("type").asText()).isEqualTo("SUBSCRIBE");
        assertFieldNames(chatListJson, "type", "chatRoomIds", "message", "timestamp");
        assertThat(chatListJson.get("type").asText()).isEqualTo("SUBSCRIBE_CHAT_LIST");
        assertFieldNames(errorJson, "type", "errorCode", "message");
        assertThat(errorJson.get("type").asText()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("기존 unread와 채팅방 목록 push 응답의 필드명을 유지한다")
    void serializeLegacyServerPushResponses() {
        WebSocketMessageDTO.UnreadCountUpdateMessage unreadCount =
                WebSocketMessageDTO.UnreadCountUpdateMessage.builder()
                        .type(WebSocketMessageType.UNREAD_COUNT_UPDATE)
                        .chatRoomId(20L)
                        .messageId(100L)
                        .newUnreadCount(1)
                        .timestamp(TIMESTAMP)
                        .build();
        WebSocketMessageDTO.UnreadStatusUpdateMessage unreadStatus =
                WebSocketMessageDTO.UnreadStatusUpdateMessage.builder()
                        .type(WebSocketMessageType.UNREAD_STATUS_UPDATE)
                        .hasUnread(true)
                        .hasPartyUnread(true)
                        .hasDirectUnread(false)
                        .timestamp(TIMESTAMP)
                        .build();
        WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate lastMessage =
                WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate.builder()
                        .content("hello")
                        .timestamp(TIMESTAMP)
                        .messageType("TEXT")
                        .build();
        WebSocketMessageDTO.ChatRoomListUpdate roomList =
                WebSocketMessageDTO.ChatRoomListUpdate.builder()
                        .type(WebSocketMessageType.CHAT_ROOM_LIST_UPDATE)
                        .chatRoomId(20L)
                        .lastMessage(lastMessage)
                        .newUnreadCount(3)
                        .timestamp(TIMESTAMP)
                        .build();

        JsonNode unreadCountJson = objectMapper.valueToTree(unreadCount);
        JsonNode unreadStatusJson = objectMapper.valueToTree(unreadStatus);
        JsonNode roomListJson = objectMapper.valueToTree(roomList);

        assertFieldNames(unreadCountJson,
                "type", "chatRoomId", "messageId", "newUnreadCount", "timestamp");
        assertThat(unreadCountJson.get("type").asText()).isEqualTo("UNREAD_COUNT_UPDATE");
        assertFieldNames(unreadStatusJson,
                "type", "hasUnread", "hasPartyUnread", "hasDirectUnread", "timestamp");
        assertThat(unreadStatusJson.get("type").asText()).isEqualTo("UNREAD_STATUS_UPDATE");
        assertFieldNames(roomListJson,
                "type", "chatRoomId", "lastMessage", "newUnreadCount", "timestamp");
        assertThat(roomListJson.get("type").asText()).isEqualTo("CHAT_ROOM_LIST_UPDATE");
        assertFieldNames(roomListJson.get("lastMessage"), "content", "timestamp", "messageType");
    }

    private void assertFieldNames(JsonNode json, String... expectedFieldNames) {
        Set<String> actualFieldNames = new HashSet<>();
        json.fieldNames().forEachRemaining(actualFieldNames::add);
        assertThat(actualFieldNames).containsExactlyInAnyOrder(expectedFieldNames);
    }
}
