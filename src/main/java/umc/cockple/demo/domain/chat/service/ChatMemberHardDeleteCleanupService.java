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

    /**
     * Member hard delete 전용 chat cleanup contract.
     * <p>
     * 회원 탈퇴(soft delete) 시에는 호출하지 않는다. 향후 회원 hard delete scheduler/orchestrator는
     * {@code memberRepository.delete(...)} 실행 전에 이 메서드를 같은 삭제 흐름에서 호출해야 한다.
     * 이 메서드는 삭제 대상 회원의 채팅 발신자/채팅방 참여자 참조를 null로 끊어 기존 채팅 기록을
     * 보존하고, 회원 개인 상태 데이터인 읽음 상태를 제거한다.
     * </p>
     * <p>
     * {@code message_read_status}는 읽음 상태 계산을 위한 ID 기반 테이블이므로 Member JPA 연관관계나
     * DB FK를 두지 않는다. 삭제 대상 회원의 읽음 상태는 이 메서드가 명시적으로 정리한다.
     * </p>
     */
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
