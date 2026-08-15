package umc.cockple.demo.domain.push.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.chat.events.ChatNotificationEvent;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPushNotificationService {

    private final FcmService fcmService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void sendPush(ChatNotificationEvent event) {
        sendPush(event, false);
    }

    public void sendPushWithRetry(ChatNotificationEvent event) {
        sendPush(event, true);
    }

    private void sendPush(ChatNotificationEvent event, boolean propagateFailure) {
        // 발신자 본인과 현재 채팅방을 보고 있는(active) 구독자는 푸시 대상에서 제외
        List<Member> recipients = chatRoomMemberRepository
                .findByChatRoomIdAndStatusWithMember(event.chatRoomId(), ChatRoomMemberStatus.JOINED)
                .stream()
                .filter(crm -> !crm.getMember().getId().equals(event.senderId()))
                .filter(crm -> !event.activeSubscriberIds().contains(crm.getMember().getId()))
                .map(crm -> crm.getMember())
                .toList();

        // 수신자 전체에게 한 번의 멀티캐스트로 전송
        if (propagateFailure) {
            fcmService.sendChatMulticastWithRetry(
                    recipients, event.notificationTitle(), event.notificationContent(),
                    event.chatRoomId(), event.chatRoomType());
        } else {
            fcmService.sendChatMulticast(
                    recipients, event.notificationTitle(), event.notificationContent(),
                    event.chatRoomId(), event.chatRoomType());
        }
    }
}
