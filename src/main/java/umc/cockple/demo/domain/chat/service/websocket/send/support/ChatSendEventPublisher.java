package umc.cockple.demo.domain.chat.service.websocket.send.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.service.ChatUnreadQueryService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.events.ChatNotificationEvent;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSendEventPublisher {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public void publishChatNotificationEvent(
            ChatRoom chatRoom,
            ChatMessage savedMessage,
            Member sender,
            List<Long> activeSubscribers) {
        String notificationTitle = chatRoom.getType() == ChatRoomType.PARTY
                ? chatRoom.getParty().getPartyName()
                : sender.getNickname();
        String notificationContent = chatRoom.getType() == ChatRoomType.PARTY
                ? sender.getNickname() + ": " + savedMessage.getDisplayContent()
                : savedMessage.getDisplayContent();

        eventPublisher.publishEvent(ChatNotificationEvent.create(
                chatRoom.getId(),
                chatRoom.getType(),
                notificationTitle,
                notificationContent,
                sender.getId(),
                activeSubscribers
        ));
    }

    public void publishChatRoomListUpdateEvent(ChatRoom chatRoom, ChatMessage savedMessage) {
        try {
            List<Long> chatRoomMemberIds = chatRoomMemberRepository.findMemberIdsByChatRoomId(chatRoom.getId());

            Map<Long, Integer> memberUnreadCounts = chatUnreadQueryService.countUnreadMessagesByMembers(
                    chatRoom.getId(), chatRoomMemberIds);

            ChatRoomListUpdateEvent listUpdateEvent = ChatRoomListUpdateEvent.create(
                    chatRoom.getId(),
                    savedMessage.getDisplayContent(),
                    savedMessage.getCreatedAt(),
                    savedMessage.getType().name(),
                    memberUnreadCounts
            );

            eventPublisher.publishEvent(listUpdateEvent);
            log.info("채팅방 목록 업데이트 이벤트 발행 - 채팅방: {}", chatRoom.getId());

        } catch (Exception e) {
            log.error("채팅방 목록 업데이트 이벤트 발행 실패 - 채팅방: {}", chatRoom.getId(), e);
        }
    }

    public void publishUnreadStatusUpdateEvent(ChatRoom chatRoom, Long senderId) {
        try {
            List<Long> targetMemberIds = chatRoomMemberRepository.findMemberIdsByChatRoomId(chatRoom.getId()).stream()
                    .filter(memberId -> senderId == null || !memberId.equals(senderId))
                    .distinct()
                    .toList();

            if (targetMemberIds.isEmpty()) {
                log.debug("안읽음 상태 업데이트 대상 없음 - 채팅방: {}", chatRoom.getId());
                return;
            }

            eventPublisher.publishEvent(ChatUnreadStatusUpdateEvent.of(targetMemberIds));
            log.info("안읽음 상태 업데이트 이벤트 발행 - 채팅방: {}, 대상자: {}명",
                    chatRoom.getId(), targetMemberIds.size());
        } catch (Exception e) {
            log.error("안읽음 상태 업데이트 이벤트 발행 실패 - 채팅방: {}", chatRoom.getId(), e);
        }
    }
}
