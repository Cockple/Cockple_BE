package umc.cockple.demo.domain.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.websocket.ChatListSubscriptionService;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.RedisSubscriptionService;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private MessageReadStatusRepository messageReadStatusRepository;
    @Mock
    private ChatRoomListCacheService chatRoomListCacheService;
    @Mock
    private RedisSubscriptionService redisSubscriptionService;
    @Mock
    private ChatListSubscriptionService chatListSubscriptionService;

    @Nested
    @DisplayName("deletePartyChatRoom")
    class DeletePartyChatRoom {

        @Test
        @DisplayName("성공 - 채팅방이 있으면 읽음 상태, 캐시/구독 정리 후 채팅방을 삭제한다")
        void success_deletePartyChatRoom() {
            Long partyId = 1L;
            Long chatRoomId = 2L;

            ChatRoom chatRoom = ChatFixture.createPartyChatRoom(
                    PartyFixture.createParty("테스트 모임", 10L, PartyFixture.createPartyAddr("서울", "강남"))
            );
            ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);

            given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.of(chatRoom));

            chatRoomService.deletePartyChatRoom(partyId);

            var inOrder = inOrder(
                    messageReadStatusRepository,
                    chatRoomListCacheService,
                    redisSubscriptionService,
                    chatListSubscriptionService,
                    chatRoomRepository
            );
            inOrder.verify(messageReadStatusRepository).deleteByChatRoomId(chatRoomId);
            inOrder.verify(chatRoomListCacheService).evictLastMessage(chatRoomId);
            inOrder.verify(redisSubscriptionService).clearRoomSubscribers(chatRoomId);
            inOrder.verify(chatListSubscriptionService).clearChatListSubscribers(chatRoomId);
            inOrder.verify(chatRoomRepository).delete(chatRoom);
        }

        @Test
        @DisplayName("성공 - 채팅방이 없으면 아무것도 삭제하지 않고 종료한다")
        void success_deletePartyChatRoom_whenRoomMissing() {
            Long partyId = 1L;
            given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.empty());

            chatRoomService.deletePartyChatRoom(partyId);

            verify(messageReadStatusRepository, never()).deleteByChatRoomId(org.mockito.ArgumentMatchers.anyLong());
            verify(chatRoomListCacheService, never()).evictLastMessage(org.mockito.ArgumentMatchers.anyLong());
            verify(redisSubscriptionService, never()).clearRoomSubscribers(org.mockito.ArgumentMatchers.anyLong());
            verify(chatListSubscriptionService, never()).clearChatListSubscribers(org.mockito.ArgumentMatchers.anyLong());
            verify(chatRoomRepository, never()).delete(org.mockito.ArgumentMatchers.any(ChatRoom.class));
        }
    }
}
