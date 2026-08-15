package umc.cockple.demo.domain.chat.service.websocket.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomMemberReader;
import umc.cockple.demo.domain.chat.service.support.reader.ChatRoomReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketRequestValidator")
class ChatWebSocketRequestValidatorTest {

    @InjectMocks
    private ChatWebSocketRequestValidator chatWebSocketRequestValidator;

    @Mock
    private ChatRoomReader chatRoomReader;

    @Mock
    private ChatRoomMemberReader chatRoomMemberReader;

    @Nested
    @DisplayName("validateSendRequest")
    class ValidateSendRequest {

        @Test
        @DisplayName("성공 - 채팅방과 권한이 있고 내용이 있으면 통과한다")
        void success_whenContentExists() {
            givenExistingRoomAndMembership(10L, 1L);

            assertThatCode(() -> chatWebSocketRequestValidator.validateSendRequest(10L, "hello", List.of(), 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("성공 - 내용이 없어도 파일이 있으면 통과한다")
        void success_whenFilesExist() {
            givenExistingRoomAndMembership(10L, 1L);
            List<WebSocketMessageDTO.Request.FileInfo> files = List.of(
                    new WebSocketMessageDTO.Request.FileInfo("chat/a.webp", 1, "a.webp", 100L, "image/webp")
            );

            assertThatCode(() -> chatWebSocketRequestValidator.validateSendRequest(10L, null, files, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("실패 - 채팅방 ID가 없으면 CHATROOM_ID_NECESSARY 예외를 던진다")
        void fail_whenChatRoomIdIsNull() {
            assertChatException(
                    ChatErrorCode.CHATROOM_ID_NECESSARY,
                    () -> chatWebSocketRequestValidator.validateSendRequest(null, "hello", List.of(), 1L)
            );
            verify(chatRoomReader, never()).exists(null);
        }

        @Test
        @DisplayName("실패 - 채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외를 던진다")
        void fail_whenChatRoomNotFound() {
            given(chatRoomReader.exists(10L)).willReturn(false);

            assertChatException(
                    ChatErrorCode.CHAT_ROOM_NOT_FOUND,
                    () -> chatWebSocketRequestValidator.validateSendRequest(10L, "hello", List.of(), 1L)
            );
            verify(chatRoomMemberReader, never()).exists(10L, 1L);
        }

        @Test
        @DisplayName("실패 - 채팅방 멤버가 아니면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
        void fail_whenNotChatRoomMember() {
            given(chatRoomReader.exists(10L)).willReturn(true);
            given(chatRoomMemberReader.exists(10L, 1L)).willReturn(false);

            assertChatException(
                    ChatErrorCode.CHAT_ROOM_ACCESS_DENIED,
                    () -> chatWebSocketRequestValidator.validateSendRequest(10L, "hello", List.of(), 1L)
            );
        }

        @Test
        @DisplayName("실패 - 내용과 파일이 모두 없으면 EMPTY_MESSAGE_NOT_ALLOWED 예외를 던진다")
        void fail_whenMessageIsEmpty() {
            givenExistingRoomAndMembership(10L, 1L);

            assertChatException(
                    ChatErrorCode.EMPTY_MESSAGE_NOT_ALLOWED,
                    () -> chatWebSocketRequestValidator.validateSendRequest(10L, "   ", List.of(), 1L)
            );
        }

        @Test
        @DisplayName("실패 - 내용이 1000자를 초과하면 MESSAGE_TO_LONG 예외를 던진다")
        void fail_whenContentTooLong() {
            givenExistingRoomAndMembership(10L, 1L);
            String tooLongContent = "a".repeat(1001);

            assertChatException(
                    ChatErrorCode.MESSAGE_TO_LONG,
                    () -> chatWebSocketRequestValidator.validateSendRequest(10L, tooLongContent, List.of(), 1L)
            );
        }
    }

    @Nested
    @DisplayName("채팅방 구독 요청 검증")
    class ValidateRoomSubscriptionRequest {

        @Test
        @DisplayName("성공 - 구독과 구독 해제는 채팅방과 권한이 있으면 통과한다")
        void success_whenRoomAndMembershipExist() {
            givenExistingRoomAndMembership(10L, 1L);

            assertThatCode(() -> chatWebSocketRequestValidator.validateSubscriptionRequest(10L, 1L))
                    .doesNotThrowAnyException();
            assertThatCode(() -> chatWebSocketRequestValidator.validateUnsubscriptionRequest(10L, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("실패 - 채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외를 던진다")
        void fail_whenChatRoomNotFound() {
            given(chatRoomReader.exists(10L)).willReturn(false);

            assertChatException(
                    ChatErrorCode.CHAT_ROOM_NOT_FOUND,
                    () -> chatWebSocketRequestValidator.validateSubscriptionRequest(10L, 1L)
            );
        }

        @Test
        @DisplayName("실패 - 권한이 없으면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
        void fail_whenNotChatRoomMember() {
            given(chatRoomReader.exists(10L)).willReturn(true);
            given(chatRoomMemberReader.exists(10L, 1L)).willReturn(false);

            assertChatException(
                    ChatErrorCode.CHAT_ROOM_ACCESS_DENIED,
                    () -> chatWebSocketRequestValidator.validateUnsubscriptionRequest(10L, 1L)
            );
        }
    }

    @Nested
    @DisplayName("채팅 목록 구독 요청 검증")
    class ValidateChatListSubscriptionRequest {

        @Test
        @DisplayName("성공 - 중복 채팅방 ID는 distinct 처리해 권한을 확인한다")
        void success_checksDistinctRoomIds() {
            given(chatRoomMemberReader.exists(10L, 1L)).willReturn(true);
            given(chatRoomMemberReader.exists(20L, 1L)).willReturn(true);

            assertThatCode(() -> chatWebSocketRequestValidator.validateChatListSubscriptionRequest(1L, List.of(10L, 10L, 20L)))
                    .doesNotThrowAnyException();

            verify(chatRoomMemberReader, times(1)).exists(10L, 1L);
            verify(chatRoomMemberReader, times(1)).exists(20L, 1L);
        }

        @Test
        @DisplayName("성공 - 구독 해제도 동일한 채팅방 목록 검증을 통과한다")
        void success_unsubscriptionUsesSameValidation() {
            given(chatRoomMemberReader.exists(10L, 1L)).willReturn(true);

            assertThatCode(() -> chatWebSocketRequestValidator.validateChatListUnsubscriptionRequest(1L, List.of(10L)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("실패 - 채팅방 목록이 null 또는 비어 있으면 CHATROOM_LIST_EMPTY 예외를 던진다")
        void fail_whenRoomIdsAreNullOrEmpty() {
            assertChatException(
                    ChatErrorCode.CHATROOM_LIST_EMPTY,
                    () -> chatWebSocketRequestValidator.validateChatListSubscriptionRequest(1L, null)
            );
            assertChatException(
                    ChatErrorCode.CHATROOM_LIST_EMPTY,
                    () -> chatWebSocketRequestValidator.validateChatListSubscriptionRequest(1L, Collections.emptyList())
            );
        }

        @Test
        @DisplayName("실패 - 채팅방 목록이 100개를 초과하면 TOO_MANY_CHATROOMS 예외를 던진다")
        void fail_whenTooManyRoomIds() {
            List<Long> tooManyRoomIds = Collections.nCopies(101, 10L);

            assertChatException(
                    ChatErrorCode.TOO_MANY_CHATROOMS,
                    () -> chatWebSocketRequestValidator.validateChatListSubscriptionRequest(1L, tooManyRoomIds)
            );
        }

        @Test
        @DisplayName("실패 - null 또는 0 이하 채팅방 ID가 있으면 INVALID_CHATROOM_ID 예외를 던진다")
        void fail_whenRoomIdIsInvalid() {
            assertChatException(
                    ChatErrorCode.INVALID_CHATROOM_ID,
                    () -> chatWebSocketRequestValidator.validateChatListSubscriptionRequest(1L, List.of(0L, 10L))
            );
            assertChatException(
                    ChatErrorCode.INVALID_CHATROOM_ID,
                    () -> chatWebSocketRequestValidator.validateChatListSubscriptionRequest(1L, Arrays.asList(null, 10L))
            );
        }

        @Test
        @DisplayName("실패 - 하나라도 접근 권한이 없으면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
        void fail_whenAnyRoomAccessDenied() {
            given(chatRoomMemberReader.exists(10L, 1L)).willReturn(true);
            given(chatRoomMemberReader.exists(20L, 1L)).willReturn(false);

            assertChatException(
                    ChatErrorCode.CHAT_ROOM_ACCESS_DENIED,
                    () -> chatWebSocketRequestValidator.validateChatListUnsubscriptionRequest(1L, List.of(10L, 20L))
            );
        }
    }

    private void givenExistingRoomAndMembership(Long chatRoomId, Long memberId) {
        given(chatRoomReader.exists(chatRoomId)).willReturn(true);
        given(chatRoomMemberReader.exists(chatRoomId, memberId)).willReturn(true);
    }

    private void assertChatException(ChatErrorCode expectedCode, ThrowingRunnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOfSatisfying(ChatException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(expectedCode));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
