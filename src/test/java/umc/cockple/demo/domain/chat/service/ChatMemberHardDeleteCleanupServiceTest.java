package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemberHardDeleteCleanupService")
class ChatMemberHardDeleteCleanupServiceTest {

    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatMemberHardDeleteCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new ChatMemberHardDeleteCleanupService(
                messageReadStatusRepository,
                chatMessageRepository,
                chatRoomMemberRepository
        );
    }

    @Test
    @DisplayName("회원 hard delete 전에 채팅 메시지/참여자 참조와 읽음 상태를 정리한다")
    void prepareMemberHardDelete_cleansChatReferences() {
        Long memberId = 10L;
        given(messageReadStatusRepository.deleteByMemberId(memberId)).willReturn(3);
        given(chatMessageRepository.clearSenderByMemberId(memberId)).willReturn(2);
        given(chatRoomMemberRepository.clearMemberByMemberId(memberId)).willReturn(1);

        ChatMemberHardDeleteCleanupService.Result result = cleanupService.prepareMemberHardDelete(memberId);

        assertThat(result.deletedReadStatuses()).isEqualTo(3);
        assertThat(result.clearedMessages()).isEqualTo(2);
        assertThat(result.clearedChatRoomMembers()).isEqualTo(1);
        verify(messageReadStatusRepository).deleteByMemberId(memberId);
        verify(chatMessageRepository).clearSenderByMemberId(memberId);
        verify(chatRoomMemberRepository).clearMemberByMemberId(memberId);
    }
}
