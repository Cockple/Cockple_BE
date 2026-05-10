package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.fcm.FcmService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPushNotificationService {

    private final FcmService fcmService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void sendPush(ChatRoom chatRoom, ChatMessage message, Member sender, List<Long> activeSubscriberIds) {
        String title = buildTitle(chatRoom, sender);
        String content = buildContent(chatRoom, sender, message);

        chatRoomMemberRepository
                .findByChatRoomIdAndStatusWithMember(chatRoom.getId(), ChatRoomMemberStatus.JOINED)
                .stream()
                .filter(crm -> !crm.getMember().getId().equals(sender.getId()))
                .filter(crm -> !activeSubscriberIds.contains(crm.getMember().getId()))
                .forEach(crm -> fcmService.sendChatNotification(
                        crm.getMember(), title, content, chatRoom.getId(), chatRoom.getType()
                ));
    }

    private String buildTitle(ChatRoom chatRoom, Member sender) {
        if (chatRoom.getType() == ChatRoomType.PARTY) {
            return chatRoom.getParty().getPartyName();
        }
        return sender.getNickname();
    }

    private String buildContent(ChatRoom chatRoom, Member sender, ChatMessage message) {
        if (chatRoom.getType() == ChatRoomType.PARTY) {
            return sender.getNickname() + ": " + message.getDisplayContent();
        }
        return message.getDisplayContent();
    }
}
