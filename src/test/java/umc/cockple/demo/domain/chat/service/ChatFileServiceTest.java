package umc.cockple.demo.domain.chat.service;

import com.google.cloud.storage.Blob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.DownloadToken;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.DownloadTokenRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatFileService 단위 테스트")
class ChatFileServiceTest {
    @Mock private ChatFileRepository chatFileRepository;
    @Mock private DownloadTokenRepository downloadTokenRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private FileService fileService;

    private ChatFileService chatFileService;

    private ChatRoom chatRoom;
    private ChatMessage message;
    private ChatMessageFile chatFile;

    @BeforeEach
    void setUp() {
        ChatConverter chatConverter = new ChatConverter();
        chatFileService = new ChatFileServiceImpl(
                chatFileRepository,
                downloadTokenRepository,
                chatConverter,
                chatRoomMemberRepository,
                fileService
        );
        Member sender = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(sender, "id", 1L);

        var party = PartyFixture.createParty("모임", sender.getId(), PartyFixture.createPartyAddr("서울", "강남구"));
        ReflectionTestUtils.setField(party, "id", 100L);

        chatRoom = ChatFixture.createPartyChatRoom(party);
        ReflectionTestUtils.setField(chatRoom, "id", 10L);

        message = ChatFixture.createTextMessage(chatRoom, sender, "파일 첨부");
        ReflectionTestUtils.setField(message, "id", 50L);

        chatFile = ChatMessageFile.create(message, "test/key.webp", 1, "test.webp", 100L, "image/webp");
        ReflectionTestUtils.setField(chatFile, "id", 100L);
    }

    // ========== issueDownloadToken ==========
    @Nested
    @DisplayName("issueDownloadToken - 다운로드 토큰 발급 테스트")
    class IssueDownloadToken {

        @Test
        @DisplayName("채팅방 권한이 있는 멤버는 토큰을 성공적으로 발급받는다")
        void success() {
            given(chatFileRepository.findById(100L)).willReturn(Optional.of(chatFile));
            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(10L, 1L)).willReturn(true);

            ChatDownloadTokenDTO.Response response = chatFileService.issueDownloadToken(100L, 1L);

            assertThat(response).isNotNull();
            assertThat(response.downloadToken()).isNotBlank();
            verify(downloadTokenRepository).save(any(DownloadToken.class));
        }

        @Test
        @DisplayName("존재하지 않는 파일 ID를 요청하면 FILE_NOT_FOUND 예외가 발생한다")
        void throwExceptionWhenFileNotFound() {
            given(chatFileRepository.findById(999L)).willReturn(Optional.empty());

            ChatException exception = assertThrows(ChatException.class, () -> chatFileService.issueDownloadToken(999L, 1L));

            assertThat(exception.getCode()).isEqualTo(ChatErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 경우 권한 부족으로 CHAT_ROOM_ACCESS_DENIED 예외가 발생한다")
        void throwExceptionWhenAccessDenied() {
            given(chatFileRepository.findById(100L)).willReturn(Optional.of(chatFile));
            given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(10L, 2L)).willReturn(false);

            ChatException exception = assertThrows(
                    ChatException.class, () -> chatFileService.issueDownloadToken(100L, 2L)
            );
            assertThat(exception.getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    // ========== downloadFile ==========
    @Nested
    @DisplayName("downloadFile - 실제 파일 다운로드 테스트")
    class DownloadFile {

        @Test
        @DisplayName("유효한 토큰 사용 시 Blob 데이터를 기반으로 정상적으로 ResponseEntity를 반환한다")
        void success() {
            DownloadToken token = DownloadToken.create(100L, 1L, 180);
            ReflectionTestUtils.setField(token, "expiresAt", LocalDateTime.now().plusMinutes(5));
            given(downloadTokenRepository.findByToken("valid-token")).willReturn(Optional.of(token));
            given(chatFileRepository.findById(100L)).willReturn(Optional.of(chatFile));
            Blob mockBlob = mock(Blob.class);
            given(mockBlob.getSize()).willReturn(1024L);
            given(mockBlob.getContentType()).willReturn("image/webp");
            given(mockBlob.getContent()).willReturn(new byte[1024]);
            given(fileService.downloadFile("test/key.webp")).willReturn(mockBlob);

            ResponseEntity<Resource> response = chatFileService.downloadFile(100L, "valid-token");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getHeaders().getContentLength()).isEqualTo(1024L);
            assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/webp");
            verify(downloadTokenRepository).delete(token);
        }

        @Test
        @DisplayName("DB에 존재하지 않거나 만료된 토큰의 경우 INVALID_DOWNLOAD_TOKEN 예외가 발생한다")
        void throwExceptionWhenInvalidToken() {
            DownloadToken token = DownloadToken.create(100L, 1L, 0);
            ReflectionTestUtils.setField(token, "expiresAt", LocalDateTime.now().minusMinutes(5)); // 만료된 토큰
            given(downloadTokenRepository.findByToken("expired-token")).willReturn(Optional.of(token));

            ChatException exception = assertThrows(ChatException.class, () ->
                    chatFileService.downloadFile(100L, "expired-token")
            );
            assertThat(exception.getCode()).isEqualTo(ChatErrorCode.INVALID_DOWNLOAD_TOKEN);
        }
    }
}