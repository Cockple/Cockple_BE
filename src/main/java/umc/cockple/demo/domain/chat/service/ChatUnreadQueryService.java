package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

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

    public int countPartyUnreadMessages(Long memberId) {
        return messageReadStatusRepository.countPartyUnreadMessagesByMemberId(memberId);
    }

    public int countDirectUnreadMessages(Long memberId) {
        return messageReadStatusRepository.countDirectUnreadMessagesByMemberId(memberId);
    }
}
