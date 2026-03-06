package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatProcessor")
class ChatProcessorTest {

    @Mock
    private FileService fileService;

    private ChatConverter chatConverter;
    private ChatProcessor chatProcessor;

    private Member sender;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        chatConverter = new ChatConverter();
        chatProcessor = new ChatProcessor(fileService, chatConverter);

        sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", 10L);

        var party = PartyFixture.createParty("모임", sender.getId(), PartyFixture.createPartyAddr("서울", "강남구"));
        ReflectionTestUtils.setField(party, "id", 100L);
        chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", 1L);
    }

    // ========== generateProfileImageUrl ==========

    @Nested
    @DisplayName("generateProfileImageUrl - 프로필 이미지 URL 생성")
    class GenerateProfileImageUrl {

        @Test
        @DisplayName("profileImg가 null이면 null을 반환한다")
        void returnsNull_whenProfileImgIsNull() {
            String result = chatProcessor.generateProfileImageUrl(null);

            assertThat(result).isNull();
            verify(fileService, never()).getUrlFromKey(null);
        }

        @Test
        @DisplayName("imgKey가 null이면 null을 반환하고 imageService를 호출하지 않는다")
        void returnsNull_whenImgKeyIsNull() {
            ProfileImg profileImg = ProfileImg.builder()
                    .imgKey(null)
                    .build();

            String result = chatProcessor.generateProfileImageUrl(profileImg);

            assertThat(result).isNull();
            verify(fileService, never()).getUrlFromKey(null);
        }

        @Test
        @DisplayName("imgKey가 공백 문자열이면 null을 반환하고 imageService를 호출하지 않는다")
        void returnsNull_whenImgKeyIsBlank() {
            ProfileImg profileImg = ProfileImg.builder()
                    .imgKey("   ")
                    .build();

            String result = chatProcessor.generateProfileImageUrl(profileImg);

            assertThat(result).isNull();
            verify(fileService, never()).getUrlFromKey("   ");
        }

        @Test
        @DisplayName("유효한 imgKey가 있으면 imageService로 URL을 생성해서 반환한다")
        void returnsUrl_whenImgKeyIsValid() {
            ProfileImg profileImg = ProfileImg.builder()
                    .imgKey("profile/key123.jpg")
                    .build();

            given(fileService.getUrlFromKey("profile/key123.jpg"))
                    .willReturn("https://cdn.example.com/profile/key123.jpg");

            String result = chatProcessor.generateProfileImageUrl(profileImg);

            assertThat(result).isEqualTo("https://cdn.example.com/profile/key123.jpg");
            verify(fileService).getUrlFromKey("profile/key123.jpg");
        }
    }

    // ========== generateImageUrl ==========

    @Nested
    @DisplayName("generateImageUrl - 채팅 이미지 URL 생성")
    class GenerateImageUrl {

        @Test
        @DisplayName("img가 null이면 null을 반환한다")
        void returnsNull_whenImgIsNull() {
            String result = chatProcessor.generateFileUrl(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("imgKey가 null이면 null을 반환하고 imageService를 호출하지 않는다")
        void returnsNull_whenImgKeyIsNull() {
            ChatMessageFile img = ChatMessageFile.builder()
                    .fileKey(null)
                    .imgOrder(1)
                    .originalFileName("photo.jpg")
                    .fileSize(1024L)
                    .fileType("image/jpeg")
                    .build();

            String result = chatProcessor.generateFileUrl(img);

            assertThat(result).isNull();
            verify(fileService, never()).getUrlFromKey(null);
        }

        @Test
        @DisplayName("imgKey가 공백 문자열이면 null을 반환하고 imageService를 호출하지 않는다")
        void returnsNull_whenImgKeyIsBlank() {
            ChatMessageFile img = ChatMessageFile.builder()
                    .fileKey("  ")
                    .imgOrder(1)
                    .originalFileName("photo.jpg")
                    .fileSize(1024L)
                    .fileType("image/jpeg")
                    .build();

            String result = chatProcessor.generateFileUrl(img);

            assertThat(result).isNull();
            verify(fileService, never()).getUrlFromKey("  ");
        }

        @Test
        @DisplayName("유효한 imgKey가 있으면 imageService로 URL을 생성해서 반환한다")
        void returnsUrl_whenImgKeyIsValid() {
            ChatMessageFile img = ChatMessageFile.builder()
                    .fileKey("chat/img456.jpg")
                    .imgOrder(1)
                    .originalFileName("photo.jpg")
                    .fileSize(2048L)
                    .fileType("image/jpeg")
                    .build();

            given(fileService.getUrlFromKey("chat/img456.jpg"))
                    .willReturn("https://cdn.example.com/chat/img456.jpg");

            String result = chatProcessor.generateFileUrl(img);

            assertThat(result).isEqualTo("https://cdn.example.com/chat/img456.jpg");
            verify(fileService).getUrlFromKey("chat/img456.jpg");
        }
    }

    // ========== processMessages ==========

    @Nested
    @DisplayName("processMessages - 메시지 목록 처리")
    class ProcessMessages {

        @Test
        @DisplayName("빈 메시지 목록을 처리하면 빈 리스트를 반환한다")
        void returnsEmptyList_whenNoMessages() {
            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("내가 보낸 메시지는 isMyMessage가 true이다")
        void isMyMessage_true_whenSenderIsCurrentUser() {
            ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "내 메시지");
            ReflectionTestUtils.setField(message, "id", 1L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isMyMessage()).isTrue();
        }

        @Test
        @DisplayName("다른 사람이 보낸 메시지는 isMyMessage가 false이다")
        void isMyMessage_false_whenSenderIsOther() {
            Member other = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
            ReflectionTestUtils.setField(other, "id", 20L);

            ChatMessage message = ChatFixture.createTextMessage(chatRoom, other, "상대방 메시지");
            ReflectionTestUtils.setField(message, "id", 1L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isMyMessage()).isFalse();
        }

        @Test
        @DisplayName("탈퇴한 회원이 보낸 메시지는 isSenderWithdrawn이 true이다")
        void isSenderWithdrawn_true_whenSenderIsInactive() {
            Member withdrawn = MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L);
            ReflectionTestUtils.setField(withdrawn, "id", 30L);

            ChatMessage message = ChatFixture.createTextMessage(chatRoom, withdrawn, "탈퇴자 메시지");
            ReflectionTestUtils.setField(message, "id", 1L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isSenderWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("활성 회원이 보낸 메시지는 isSenderWithdrawn이 false이다")
        void isSenderWithdrawn_false_whenSenderIsActive() {
            ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "활성 사용자 메시지");
            ReflectionTestUtils.setField(message, "id", 1L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            assertThat(result.get(0).isSenderWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("메시지 처리 결과에 senderName, content, messageType이 올바르게 매핑된다")
        void messageFields_areCorrectlyMapped() {
            ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "안녕하세요");
            ReflectionTestUtils.setField(message, "id", 1L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            ChatCommonDTO.MessageInfo info = result.get(0);
            assertThat(info.messageId()).isEqualTo(1L);
            assertThat(info.senderId()).isEqualTo(sender.getId());
            assertThat(info.senderName()).isEqualTo("홍길동");
            assertThat(info.content()).isEqualTo("안녕하세요");
            assertThat(info.messageType()).isEqualTo(MessageType.TEXT);
        }

        @Test
        @DisplayName("발신자에게 프로필 이미지 키가 없으면 senderProfileImageUrl이 null이다")
        void senderProfileImageUrl_isNull_whenNoProfileImg() {
            // sender는 profileImg가 null인 상태 (MemberFixture 기본)
            ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "메시지");
            ReflectionTestUtils.setField(message, "id", 1L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            assertThat(result.get(0).senderProfileImageUrl()).isNull();
            verify(fileService, never()).getUrlFromKey(null);
        }

        @Test
        @DisplayName("여러 메시지를 처리하면 입력 순서가 유지된다")
        void multipleMessages_preserveInputOrder() {
            Member other = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
            ReflectionTestUtils.setField(other, "id", 20L);

            ChatMessage msg1 = ChatFixture.createTextMessage(chatRoom, sender, "첫 번째");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            ChatMessage msg2 = ChatFixture.createTextMessage(chatRoom, other, "두 번째");
            ReflectionTestUtils.setField(msg2, "id", 2L);
            ChatMessage msg3 = ChatFixture.createTextMessage(chatRoom, sender, "세 번째");
            ReflectionTestUtils.setField(msg3, "id", 3L);

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(msg1, msg2, msg3));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).messageId()).isEqualTo(1L);
            assertThat(result.get(0).isMyMessage()).isTrue();
            assertThat(result.get(1).messageId()).isEqualTo(2L);
            assertThat(result.get(1).isMyMessage()).isFalse();
            assertThat(result.get(2).messageId()).isEqualTo(3L);
            assertThat(result.get(2).isMyMessage()).isTrue();
        }

        @Test
        @DisplayName("이미지가 포함된 메시지는 imgOrder 오름차순으로 정렬된다")
        void messageImages_areSortedByImgOrder() {
            ChatMessage message = ChatFixture.createTextMessage(chatRoom, sender, "이미지 메시지");
            ReflectionTestUtils.setField(message, "id", 1L);

            // imgOrder 역순으로 삽입: 3 → 1 → 2
            ChatMessageFile img3 = ChatMessageFile.builder()
                    .fileKey("img/third.jpg")
                    .imgOrder(3)
                    .originalFileName("third.jpg")
                    .fileSize(100L)
                    .fileType("image/jpeg")
                    .build();
            ChatMessageFile img1 = ChatMessageFile.builder()
                    .fileKey("img/first.jpg")
                    .imgOrder(1)
                    .originalFileName("first.jpg")
                    .fileSize(100L)
                    .fileType("image/jpeg")
                    .build();
            ChatMessageFile img2 = ChatMessageFile.builder()
                    .fileKey("img/second.jpg")
                    .imgOrder(2)
                    .originalFileName("second.jpg")
                    .fileSize(100L)
                    .fileType("image/jpeg")
                    .build();

            // ChatMessage의 chatMessageImgs에 역순으로 세팅
            ReflectionTestUtils.setField(message, "chatMessageImgs", List.of(img3, img1, img2));

            given(fileService.getUrlFromKey("img/first.jpg")).willReturn("https://cdn.example.com/first.jpg");
            given(fileService.getUrlFromKey("img/second.jpg")).willReturn("https://cdn.example.com/second.jpg");
            given(fileService.getUrlFromKey("img/third.jpg")).willReturn("https://cdn.example.com/third.jpg");

            List<ChatCommonDTO.MessageInfo> result = chatProcessor.processMessages(sender.getId(), List.of(message));

            List<ChatCommonDTO.FileInfo> images = result.get(0).files();
            assertThat(images).hasSize(3);
            assertThat(images.get(0).fileOrder()).isEqualTo(1);
            assertThat(images.get(1).fileOrder()).isEqualTo(2);
            assertThat(images.get(2).fileOrder()).isEqualTo(3);
        }
    }
}
