package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReadProcessingService {

    private final MessageReadStatusRepository messageReadStatusRepository;

    public List<MessageUnreadUpdate> processUnreadMessagesOnSubscribe(Long chatRoomId, Long memberId) {
        log.info("구독 시 안읽은 메시지 처리 시작 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        List<Long> unreadMessageIds = messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);

    }

    public record MessageUnreadUpdate(
            Long messageId,
            int newUnreadCount
    ) {}
}
