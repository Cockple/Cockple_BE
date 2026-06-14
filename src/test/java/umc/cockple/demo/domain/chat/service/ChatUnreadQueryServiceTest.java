package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.projection.ChatRoomUnreadCountDTO;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatUnreadQueryService")
class ChatUnreadQueryServiceTest {

    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatUnreadQueryService chatUnreadQueryService;

    @BeforeEach
    void setUp() {
        chatUnreadQueryService = new ChatUnreadQueryService(messageReadStatusRepository, chatRoomMemberRepository);
    }

    @Nested
    @DisplayName("countUnreadMessages")
    class CountUnreadMessages {

        @Test
        @DisplayName("마지막으로 읽은 메시지가 없으면 전체 안읽음 수를 사용한다")
        void usesAllUnreadMessages_whenLastReadMessageIdIsNull() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            ChatRoomMember chatRoomMember = joinedMember(roomId, memberId, null);
            given(messageReadStatusRepository.countAllUnreadMessages(roomId, memberId)).willReturn(4);

            // when
            int result = chatUnreadQueryService.countUnreadMessages(chatRoomMember);

            // then
            assertThat(result).isEqualTo(4);
            verify(messageReadStatusRepository).countAllUnreadMessages(roomId, memberId);
            verify(messageReadStatusRepository, never()).countUnreadMessagesAfter(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("마지막으로 읽은 메시지가 있으면 이후 안읽음 수를 사용한다")
        void usesUnreadMessagesAfter_whenLastReadMessageIdExists() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            ChatRoomMember chatRoomMember = joinedMember(roomId, memberId, 30L);
            given(messageReadStatusRepository.countUnreadMessagesAfter(roomId, memberId, 30L)).willReturn(2);

            // when
            int result = chatUnreadQueryService.countUnreadMessages(chatRoomMember);

            // then
            assertThat(result).isEqualTo(2);
            verify(messageReadStatusRepository).countUnreadMessagesAfter(roomId, memberId, 30L);
            verify(messageReadStatusRepository, never()).countAllUnreadMessages(roomId, memberId);
        }

        @Test
        @DisplayName("멤버십이 없으면 0으로 계산할 수 있다")
        void countUnreadMessagesOrZero_returnsZero_whenMembershipIsMissing() {
            // given
            Long roomId = 1L;
            Long memberId = 10L;
            given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)).willReturn(Optional.empty());

            // when
            int result = chatUnreadQueryService.countUnreadMessagesOrZero(roomId, memberId);

            // then
            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("batch countUnreadMessages")
    class BatchCountUnreadMessages {

        @Test
        @DisplayName("채팅방별 안읽음 수를 조회하고 결과가 없는 채팅방은 0으로 채운다")
        void countUnreadMessagesByChatRooms_fillsMissingRoomsWithZero() {
            // given
            Long memberId = 10L;
            List<Long> chatRoomIds = List.of(1L, 2L, 3L);
            given(messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, chatRoomIds))
                    .willReturn(List.of(
                            new ChatRoomUnreadCountDTO(1L, 4L),
                            new ChatRoomUnreadCountDTO(3L, 2L)
                    ));

            // when
            Map<Long, Integer> result = chatUnreadQueryService.countUnreadMessagesByChatRooms(memberId, chatRoomIds);

            // then
            assertThat(result).containsEntry(1L, 4)
                    .containsEntry(2L, 0)
                    .containsEntry(3L, 2);
        }

        @Test
        @DisplayName("입력 목록이 비어 있으면 repository를 호출하지 않고 빈 Map을 반환한다")
        void countUnreadMessagesByChatRooms_returnsEmptyMap_whenChatRoomIdsAreEmpty() {
            // when
            Map<Long, Integer> result = chatUnreadQueryService.countUnreadMessagesByChatRooms(10L, List.of());

            // then
            assertThat(result).isEmpty();
            verify(messageReadStatusRepository, never()).countUnreadMessagesByChatRooms(anyLong(), anyList());
        }
    }

    @Nested
    @DisplayName("타입별 안읽음 여부")
    class HasUnreadMessagesByType {

        @Test
        @DisplayName("모임 채팅 안읽음 여부는 repository exists 쿼리를 사용한다")
        void hasPartyUnreadMessages_usesRepositoryExists() {
            // given
            Long memberId = 10L;
            given(messageReadStatusRepository.existsPartyUnreadMessagesByMemberId(memberId)).willReturn(true);

            // when
            boolean result = chatUnreadQueryService.hasPartyUnreadMessages(memberId);

            // then
            assertThat(result).isTrue();
            verify(messageReadStatusRepository).existsPartyUnreadMessagesByMemberId(memberId);
        }

        @Test
        @DisplayName("개인 채팅 안읽음 여부는 repository exists 쿼리를 사용한다")
        void hasDirectUnreadMessages_usesRepositoryExists() {
            // given
            Long memberId = 10L;
            given(messageReadStatusRepository.existsDirectUnreadMessagesByMemberId(memberId)).willReturn(false);

            // when
            boolean result = chatUnreadQueryService.hasDirectUnreadMessages(memberId);

            // then
            assertThat(result).isFalse();
            verify(messageReadStatusRepository).existsDirectUnreadMessagesByMemberId(memberId);
        }
    }

    private ChatRoomMember joinedMember(Long roomId, Long memberId, Long lastReadMessageId) {
        Member member = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", memberId);

        ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        ChatRoomMember chatRoomMember = ChatFixture.createJoinedMember(chatRoom, member);
        if (lastReadMessageId != null) {
            chatRoomMember.updateLastReadMessageId(lastReadMessageId);
        }
        return chatRoomMember;
    }
}
