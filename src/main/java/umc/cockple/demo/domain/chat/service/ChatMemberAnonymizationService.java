package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMemberAnonymizationService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public int anonymizeDirectDisplayNames(Long memberId) {
        List<Long> directChatRoomIds = chatRoomMemberRepository.findDirectChatRoomIdsByMemberId(memberId);
        if (directChatRoomIds.isEmpty()) {
            return 0;
        }

        return chatRoomMemberRepository.updateDisplayNameByChatRoomIds(
                directChatRoomIds,
                ChatConverter.UNKNOWN_USER_NAME
        );
    }
}
