package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.notification.events.ChatNotificationEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPushNotificationService {

    private final FcmService fcmService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional(readOnly = true)
    public void sendPush(ChatNotificationEvent event) {
        chatRoomMemberRepository
                .findByChatRoomIdAndStatusWithMember(event.chatRoomId(), ChatRoomMemberStatus.JOINED)
                .stream()
                .filter(crm -> !crm.getMember().getId().equals(event.senderId()))
                .filter(crm -> !event.activeSubscriberIds().contains(crm.getMember().getId()))
                .forEach(crm -> fcmService.sendChatNotification(
                        crm.getMember(), event.notificationTitle(), event.notificationContent(),
                        event.chatRoomId(), event.chatRoomType()
                ));
    }
}
