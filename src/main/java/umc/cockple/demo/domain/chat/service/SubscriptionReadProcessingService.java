package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReadProcessingService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public List<MessageUnreadUpdate> processUnreadMessagesOnSubscribe(Long chatRoomId, Long memberId) {
        log.info("구독 시 안읽은 메시지 처리 시작 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);

        List<Long> unreadMessageIds = messageReadStatusRepository.findUnreadMessageIdsByMember(chatRoomId, memberId);

        if (unreadMessageIds.isEmpty()) {
            log.debug("처리할 안읽은 메시지가 없음 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);
            return List.of();
        }
        log.debug("처리할 안읽은 메시지 수: {} - 채팅방: {}, 멤버: {}", unreadMessageIds.size(), chatRoomId, memberId);

        List<MessageUnreadUpdate> updates = unreadMessageIds.stream()
                .map(messageId -> {
                    log.debug("메시지 읽음 처리 중 - 메시지: {}, 멤버: {}", messageId, memberId);
                    int processedCount = messageReadStatusRepository.markAsReadInMembers(messageId, List.of(memberId));
                    int newUnreadCount = messageReadStatusRepository.countUnreadByMessageId(messageId);
                    log.debug("메시지 읽음 처리 완료 - 메시지: {}, 처리 결과: {}, 새 안읽은 수: {}",
                            messageId, processedCount > 0 ? "성공" : "이미 읽음", newUnreadCount);

                    return new MessageUnreadUpdate(messageId, newUnreadCount);
                })
                .toList();

        if (!unreadMessageIds.isEmpty()) {
            Long latestMessageId = unreadMessageIds.get(unreadMessageIds.size() - 1);
            updateLastReadMessageId(chatRoomId, memberId, latestMessageId);
            log.debug("lastReadMessageId 업데이트 완료 - 채팅방: {}, 멤버: {}, 최신 메시지: {}",
                    chatRoomId, memberId, latestMessageId);
        }

        log.info("구독 시 안읽은 메시지 처리 완료 - 채팅방: {}, 멤버: {}, 처리된 메시지 수: {}",
                chatRoomId, memberId, updates.size());
        return updates;
    }

    private void updateLastReadMessageId(Long chatRoomId, Long memberId, Long messageId) {
        try {
            Optional<ChatRoomMember> chatRoomMemberOpt =
                    chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);

            if (chatRoomMemberOpt.isPresent()) {
                ChatRoomMember chatRoomMember = chatRoomMemberOpt.get();

                if (chatRoomMember.getLastReadMessageId() == null ||
                        messageId > chatRoomMember.getLastReadMessageId()) {

                    Long previousLastRead = chatRoomMember.getLastReadMessageId();
                    chatRoomMember.updateLastReadMessageId(messageId);

                    log.debug("구독 시 lastReadMessageId 업데이트 - 멤버: {}, 이전: {}, 새로운: {}",
                            memberId, previousLastRead, messageId);
                }
            } else {
                log.warn("ChatRoomMember를 찾을 수 없음 - 채팅방: {}, 멤버: {}", chatRoomId, memberId);
            }
        } catch (Exception e) {
            log.error("구독 시 lastReadMessageId 업데이트 실패 - 멤버: {}, 메시지: {}", memberId, messageId, e);
        }
    }

    public record MessageUnreadUpdate(
            Long messageId,
            int newUnreadCount
    ) {}
}
