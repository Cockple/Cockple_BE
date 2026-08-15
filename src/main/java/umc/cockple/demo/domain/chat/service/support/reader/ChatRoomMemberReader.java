package umc.cockple.demo.domain.chat.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomMemberReader {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public boolean exists(Long chatRoomId, Long memberId) {
        return Boolean.TRUE.equals(
                chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId));
    }
}
