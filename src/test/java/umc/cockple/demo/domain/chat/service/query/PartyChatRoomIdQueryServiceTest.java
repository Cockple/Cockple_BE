package umc.cockple.demo.domain.chat.service.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyChatRoomIdQueryService")
class PartyChatRoomIdQueryServiceTest {

    @Mock private PartyRepository partyRepository;
    @Mock private MemberPartyRepository memberPartyRepository;

    private PartyChatRoomIdQueryService partyChatRoomIdQueryService;

    @BeforeEach
    void setUp() {
        partyChatRoomIdQueryService = new PartyChatRoomIdQueryService(
                partyRepository,
                memberPartyRepository,
                new ChatConverter()
        );
    }

    @Nested
    @DisplayName("모임 채팅방 ID 조회")
    class GetChatRoomId {

        @Test
        @DisplayName("모임 회원이면 연결된 채팅방 ID를 반환한다")
        void returnsChatRoomId_whenMemberBelongsToParty() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;
            Long roomId = 100L;
            Party party = createParty(partyId);
            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(party);
            ReflectionTestUtils.setField(chatRoom, "id", roomId);
            ReflectionTestUtils.setField(party, "chatRoom", chatRoom);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyIdAndMemberId(partyId, memberId)).willReturn(true);

            // when
            PartyChatRoomIdDTO result = partyChatRoomIdQueryService.getChatRoomId(partyId, memberId);

            // then
            assertThat(result.roomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("존재하지 않는 모임이면 PARTY_NOT_FOUND 예외를 던진다")
        void throwsPartyNotFound_whenPartyDoesNotExist() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;
            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyChatRoomIdQueryService.getChatRoomId(partyId, memberId))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.PARTY_NOT_FOUND));
            verify(memberPartyRepository, never()).existsByPartyIdAndMemberId(partyId, memberId);
        }

        @Test
        @DisplayName("모임 회원이 아니면 NOT_PARTY_MEMBER 예외를 던진다")
        void throwsNotPartyMember_whenMemberDoesNotBelongToParty() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;
            Party party = createParty(partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyIdAndMemberId(partyId, memberId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> partyChatRoomIdQueryService.getChatRoomId(partyId, memberId))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.NOT_PARTY_MEMBER));
        }

        @Test
        @DisplayName("모임에 연결된 채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외를 던진다")
        void throwsChatRoomNotFound_whenPartyHasNoChatRoom() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;
            Party party = createParty(partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyIdAndMemberId(partyId, memberId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> partyChatRoomIdQueryService.getChatRoomId(partyId, memberId))
                    .isInstanceOf(ChatException.class)
                    .satisfies(e -> assertThat(((ChatException) e).getCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        }
    }

    private Party createParty(Long partyId) {
        Party party = PartyFixture.createParty("모임", 10L, PartyFixture.createPartyAddr("서울", "강남구"));
        ReflectionTestUtils.setField(party, "id", partyId);
        return party;
    }
}
