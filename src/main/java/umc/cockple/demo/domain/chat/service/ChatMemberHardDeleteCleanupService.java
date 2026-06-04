package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMemberHardDeleteCleanupService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public Result prepareMemberHardDelete(Long memberId) {
        int deletedReadStatuses = messageReadStatusRepository.deleteByMemberId(memberId);
        int clearedMessages = chatMessageRepository.clearSenderByMemberId(memberId);
        int clearedChatRoomMembers = chatRoomMemberRepository.clearMemberByMemberId(memberId);

        return new Result(clearedMessages, clearedChatRoomMembers, deletedReadStatuses);
    }

    public record Result(
            int clearedMessages,
            int clearedChatRoomMembers,
            int deletedReadStatuses
    ) {
    }
}
