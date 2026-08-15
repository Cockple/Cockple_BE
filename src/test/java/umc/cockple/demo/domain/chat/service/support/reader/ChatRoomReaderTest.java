package umc.cockple.demo.domain.chat.service.support.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomReader")
class ChatRoomReaderTest {

    @Mock private ChatRoomRepository chatRoomRepository;

    private ChatRoomReader chatRoomReader;

    @BeforeEach
    void setUp() {
        chatRoomReader = new ChatRoomReader(chatRoomRepository);
    }

    @Test
    @DisplayName("채팅방 ID로 채팅방을 조회한다")
    void read_returnsChatRoom() {
        // given
        Long chatRoomId = 1L;
        ChatRoom chatRoom = ChatFixture.createDirectChatRoom();
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));

        // when
        ChatRoom result = chatRoomReader.read(chatRoomId);

        // then
        assertThat(result).isSameAs(chatRoom);
    }

    @Test
    @DisplayName("채팅방 ID 조회 결과가 없으면 CHAT_ROOM_NOT_FOUND 예외를 던진다")
    void read_throwsWhenChatRoomNotFound() {
        // given
        Long chatRoomId = 1L;
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomReader.read(chatRoomId))
                .isInstanceOfSatisfying(ChatException.class, exception ->
                        assertThat(exception.getErrorReason().getCode())
                                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("모임 ID로 채팅방을 조회한다")
    void readByPartyId_returnsChatRoom() {
        // given
        Long partyId = 10L;
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("모임", 1L, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.of(chatRoom));

        // when
        ChatRoom result = chatRoomReader.readByPartyId(partyId);

        // then
        assertThat(result).isSameAs(chatRoom);
    }

    @Test
    @DisplayName("모임 ID 조회 결과가 없으면 CHAT_ROOM_NOT_FOUND 예외를 던진다")
    void readByPartyId_throwsWhenChatRoomNotFound() {
        // given
        Long partyId = 10L;
        given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomReader.readByPartyId(partyId))
                .isInstanceOfSatisfying(ChatException.class, exception ->
                        assertThat(exception.getErrorReason().getCode())
                                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("모임 ID로 채팅방을 선택 조회한다")
    void findByPartyId_returnsChatRoom() {
        // given
        Long partyId = 10L;
        ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                PartyFixture.createParty("모임", 1L, PartyFixture.createPartyAddr("서울", "강남구"))
        );
        given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.of(chatRoom));

        // when
        Optional<ChatRoom> result = chatRoomReader.findByPartyId(partyId);

        // then
        assertThat(result).containsSame(chatRoom);
    }

    @Test
    @DisplayName("모임 ID로 선택 조회한 채팅방이 없으면 빈 Optional을 반환한다")
    void findByPartyId_returnsEmptyWhenChatRoomNotFound() {
        // given
        Long partyId = 10L;
        given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.empty());

        // when
        Optional<ChatRoom> result = chatRoomReader.findByPartyId(partyId);

        // then
        assertThat(result).isEmpty();
    }
}
