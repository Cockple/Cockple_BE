package umc.cockple.demo.domain.chat.service.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectChatRoomActivationService")
class DirectChatRoomActivationServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    private DirectChatRoomActivationService directChatRoomActivationService;

    @BeforeEach
    void setUp() {
        directChatRoomActivationService =
                new DirectChatRoomActivationService(chatMessageRepository, chatRoomMemberRepository);
    }

    @Test
    @DisplayName("개인 채팅방 첫 메시지면 대기 중인 상대 멤버를 JOINED로 변경한다")
    void joinPendingMemberOnFirstMessage_joinsPendingMember() {
        // given
        Long chatRoomId = 10L;
        Long senderId = 101L;
        Member receiver = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
        ReflectionTestUtils.setField(receiver, "id", 102L);

        ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);

        ChatRoomMember pendingMember = ChatRoomMember.createPending(chatRoom, receiver, "철수");

        given(chatMessageRepository.countByChatRoomId(chatRoomId)).willReturn(1);
        given(chatRoomMemberRepository.findPendingMemberInDirect(chatRoomId, senderId))
                .willReturn(Optional.of(pendingMember));

        // when
        directChatRoomActivationService.joinPendingMemberOnFirstMessage(chatRoom, senderId);

        // then
        assertThat(pendingMember.getStatus()).isEqualTo(ChatRoomMemberStatus.JOINED);
    }

    @Test
    @DisplayName("모임 채팅방이면 첫 메시지 여부를 조회하지 않는다")
    void joinPendingMemberOnFirstMessage_ignoresPartyChatRoom() {
        // given
        Long senderId = 101L;
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("모임", senderId, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        ReflectionTestUtils.setField(chatRoom, "id", 10L);

        // when
        directChatRoomActivationService.joinPendingMemberOnFirstMessage(chatRoom, senderId);

        // then
        then(chatMessageRepository).shouldHaveNoInteractions();
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("개인 채팅방이어도 첫 메시지가 아니면 대기 멤버를 조회하지 않는다")
    void joinPendingMemberOnFirstMessage_ignoresNonFirstMessage() {
        // given
        Long chatRoomId = 10L;
        Long senderId = 101L;
        ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);

        given(chatMessageRepository.countByChatRoomId(chatRoomId)).willReturn(2);

        // when
        directChatRoomActivationService.joinPendingMemberOnFirstMessage(chatRoom, senderId);

        // then
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }
}
