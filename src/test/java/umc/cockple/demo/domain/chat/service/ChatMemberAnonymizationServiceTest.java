package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static umc.cockple.demo.domain.chat.converter.ChatConverter.UNKNOWN_USER_NAME;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemberAnonymizationService")
class ChatMemberAnonymizationServiceTest {

    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatMemberAnonymizationService anonymizationService;

    @BeforeEach
    void setUp() {
        anonymizationService = new ChatMemberAnonymizationService(chatRoomMemberRepository);
    }

    @Test
    @DisplayName("회원이 속한 direct 채팅방 표시명 스냅샷을 알 수 없는 사용자로 익명화한다")
    void anonymizeDirectDisplayNames_updatesDirectChatRoomDisplayNames() {
        Long memberId = 10L;
        List<Long> directChatRoomIds = List.of(100L, 200L);
        given(chatRoomMemberRepository.findDirectChatRoomIdsByMemberId(memberId)).willReturn(directChatRoomIds);
        given(chatRoomMemberRepository.updateDisplayNameByChatRoomIds(directChatRoomIds, UNKNOWN_USER_NAME))
                .willReturn(4);

        int result = anonymizationService.anonymizeDirectDisplayNames(memberId);

        assertThat(result).isEqualTo(4);
        verify(chatRoomMemberRepository).findDirectChatRoomIdsByMemberId(memberId);
        verify(chatRoomMemberRepository).updateDisplayNameByChatRoomIds(directChatRoomIds, UNKNOWN_USER_NAME);
    }

    @Test
    @DisplayName("회원이 속한 direct 채팅방이 없으면 표시명 업데이트를 건너뛴다")
    void anonymizeDirectDisplayNames_skipsUpdate_whenNoDirectRooms() {
        Long memberId = 10L;
        given(chatRoomMemberRepository.findDirectChatRoomIdsByMemberId(memberId)).willReturn(List.of());

        int result = anonymizationService.anonymizeDirectDisplayNames(memberId);

        assertThat(result).isZero();
        verify(chatRoomMemberRepository).findDirectChatRoomIdsByMemberId(memberId);
        verify(chatRoomMemberRepository, never()).updateDisplayNameByChatRoomIds(anyList(), anyString());
    }
}
