package umc.cockple.demo.domain.chat.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatWebSocketResponseAssembler")
class ChatWebSocketResponseAssemblerTest {

    private final ChatWebSocketResponseAssembler assembler = new ChatWebSocketResponseAssembler();

    @Test
    @DisplayName("일반 WebSocket 메시지 응답에는 TEXT messageType이 포함된다")
    void toSendMessageResponse_containsTextMessageType() {
        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", 10L);

        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("모임", sender.getId(), PartyFixture.createPartyAddr("서울", "강남구"))
        );
        ReflectionTestUtils.setField(chatRoom, "id", 1L);

        ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "안녕하세요");
        ReflectionTestUtils.setField(message, "id", 100L);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 5, 21, 23, 0));

        WebSocketMessageDTO.MessageResponse response = assembler.toSendMessageResponse(
                chatRoom.getId(),
                "안녕하세요",
                List.of(ChatCommonDTO.FileInfo.builder().imageId(1L).imageUrl("https://cdn.example.com/1").build()),
                message,
                sender,
                "https://cdn.example.com/profile",
                3
        );

        assertThat(response.type()).isNotNull();
        assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(response.senderId()).isEqualTo(sender.getId());
        assertThat(response.senderName()).isEqualTo(sender.getMemberName());
    }

    @Test
    @DisplayName("시스템 WebSocket 메시지 응답에는 SYSTEM messageType이 포함된다")
    void toSystemMessageResponse_containsSystemMessageType() {
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("모임", 1L, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        ReflectionTestUtils.setField(chatRoom, "id", 2L);

        ChatMessage systemMessage = ChatFixture.createSystemMessage(chatRoom, "홍길동님이 모임에 참여하셨습니다.");
        ReflectionTestUtils.setField(systemMessage, "id", 200L);
        ReflectionTestUtils.setField(systemMessage, "createdAt", LocalDateTime.of(2026, 5, 21, 23, 5));

        WebSocketMessageDTO.MessageResponse response = assembler.toSystemMessageResponse(
                chatRoom.getId(),
                systemMessage.getContent(),
                systemMessage
        );

        assertThat(response.type()).isNotNull();
        assertThat(response.messageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(response.senderId()).isNull();
        assertThat(response.senderName()).isEqualTo("시스템");
    }
}
