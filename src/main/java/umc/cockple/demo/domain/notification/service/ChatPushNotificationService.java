package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    // @Transactional 두지 않음: 수신자마다 FCM(외부 I/O)을 호출하는 루프 동안 DB 커넥션을 점유하지 않기 위함.
    // 조회(findBy...WithMember)는 리포지토리 자체 트랜잭션에서 멤버를 fetch join으로 로딩해 반환하고,
    // 이후 필터·FCM 전송은 detached 상태에서 커넥션 없이 수행된다.
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
