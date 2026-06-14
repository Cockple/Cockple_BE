package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatUnreadQueryService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public int countUnreadMessages(ChatRoomMember chatRoomMember) {
        Long chatRoomId = chatRoomMember.getChatRoom().getId();
        Long memberId = chatRoomMember.getMember().getId();
        Long lastReadMessageId = chatRoomMember.getLastReadMessageId();

        if (lastReadMessageId == null) {
            return messageReadStatusRepository.countAllUnreadMessages(chatRoomId, memberId);
        }
        return messageReadStatusRepository.countUnreadMessagesAfter(chatRoomId, memberId, lastReadMessageId);
    }

    public int countUnreadMessagesOrZero(Long chatRoomId, Long memberId) {
        return chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .map(this::countUnreadMessages)
                .orElse(0);
    }

    public Map<Long, Integer> countUnreadMessagesByChatRooms(Long memberId, List<Long> chatRoomIds) {
        Map<Long, Integer> unreadCounts = initializeZeroCountMap(chatRoomIds);
        if (chatRoomIds.isEmpty()) {
            return unreadCounts;
        }

        messageReadStatusRepository.countUnreadMessagesByChatRooms(memberId, chatRoomIds)
                .forEach(count -> unreadCounts.put(count.chatRoomId(), count.unreadCount().intValue()));

        return unreadCounts;
    }

    public boolean hasPartyUnreadMessages(Long memberId) {
        return messageReadStatusRepository.existsPartyUnreadMessagesByMemberId(memberId);
    }

    public boolean hasDirectUnreadMessages(Long memberId) {
        return messageReadStatusRepository.existsDirectUnreadMessagesByMemberId(memberId);
    }

    private Map<Long, Integer> initializeZeroCountMap(List<Long> ids) {
        Map<Long, Integer> unreadCounts = new LinkedHashMap<>();
        ids.forEach(id -> unreadCounts.put(id, 0));
        return unreadCounts;
    }
}
