package umc.cockple.demo.domain.chat.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessageFileAppender")
class ChatMessageFileAppenderTest {

    private final ChatMessageFileAppender chatMessageFileAppender = new ChatMessageFileAppender();

    @Test
    @DisplayName("첨부 파일 요청이 없으면 메시지 파일 목록을 변경하지 않는다")
    void append_doesNothing_whenFilesAreEmpty() {
        // given
        ChatMessage message = createMessage();

        // when
        chatMessageFileAppender.append(message, List.of());

        // then
        assertThat(message.getChatMessageFiles()).isEmpty();
    }

    @Test
    @DisplayName("첨부 파일 요청을 ChatMessageFile로 변환해 메시지에 추가한다")
    void append_addsMessageFiles() {
        // given
        ChatMessage message = createMessage();
        WebSocketMessageDTO.Request.FileInfo fileInfo = WebSocketMessageDTO.Request.FileInfo.builder()
                .imgKey("chat/image.png")
                .imgOrder(2)
                .originalFileName("image.png")
                .fileSize(2048L)
                .fileType("image/png")
                .build();

        // when
        chatMessageFileAppender.append(message, List.of(fileInfo));

        // then
        assertThat(message.getChatMessageFiles()).hasSize(1);
        ChatMessageFile appendedFile = message.getChatMessageFiles().get(0);
        assertThat(appendedFile.getChatMessage()).isSameAs(message);
        assertThat(appendedFile.getFileKey()).isEqualTo("chat/image.png");
        assertThat(appendedFile.getFileOrder()).isEqualTo(2);
        assertThat(appendedFile.getOriginalFileName()).isEqualTo("image.png");
        assertThat(appendedFile.getFileSize()).isEqualTo(2048L);
        assertThat(appendedFile.getFileType()).isEqualTo("image/png");
    }

    private ChatMessage createMessage() {
        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", 10L);
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("모임", sender.getId(), PartyFixture.createPartyAddr("서울", "강남구"))
        );
        return ChatFixture.createTextMessage(chatRoom, sender, "안녕하세요");
    }
}
