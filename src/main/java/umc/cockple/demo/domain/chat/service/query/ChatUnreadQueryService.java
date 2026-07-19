package umc.cockple.demo.domain.chat.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatUnreadQueryService {

    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatConverter chatConverter;

    public ChatUnreadStatusDTO.Response getUnreadStatus(Long memberId) {
        log.info("[채팅 안읽음 여부 조회 시작]- 요청자: {}", memberId);
        boolean hasPartyUnread = hasPartyUnreadMessages(memberId);
        boolean hasDirectUnread = hasDirectUnreadMessages(memberId);
        log.info("[채팅 안읽음 여부 조회 완료]- hasUnread: {}", hasPartyUnread || hasDirectUnread);

        return chatConverter.toUnreadStatusResponse(hasPartyUnread, hasDirectUnread);
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

    public Map<Long, Integer> countUnreadMessagesByMembers(Long chatRoomId, List<Long> memberIds) {
        Map<Long, Integer> unreadCounts = initializeZeroCountMap(memberIds);
        if (memberIds.isEmpty()) {
            return unreadCounts;
        }

        messageReadStatusRepository.countUnreadMessagesByMembers(chatRoomId, memberIds)
                .forEach(count -> unreadCounts.put(count.memberId(), count.unreadCount().intValue()));

        return unreadCounts;
    }

    public boolean hasPartyUnreadMessages(Long memberId) {
        return messageReadStatusRepository.existsPartyUnreadMessagesByMemberId(memberId);
    }

    public boolean hasDirectUnreadMessages(Long memberId) {
        return messageReadStatusRepository.existsDirectUnreadMessagesByMemberId(memberId);
    }

    public Map<Long, UnreadStatus> findUnreadStatusesByMembers(List<Long> memberIds) {
        Map<Long, UnreadStatus> unreadStatuses = initializeUnreadStatusMap(memberIds);
        if (unreadStatuses.isEmpty()) {
            return unreadStatuses;
        }

        List<Long> distinctMemberIds = List.copyOf(unreadStatuses.keySet());
        Set<Long> partyUnreadMemberIds = new HashSet<>(
                messageReadStatusRepository.findMemberIdsWithPartyUnreadMessages(distinctMemberIds));
        Set<Long> directUnreadMemberIds = new HashSet<>(
                messageReadStatusRepository.findMemberIdsWithDirectUnreadMessages(distinctMemberIds));

        unreadStatuses.replaceAll((memberId, ignored) -> new UnreadStatus(
                partyUnreadMemberIds.contains(memberId),
                directUnreadMemberIds.contains(memberId)
        ));

        return unreadStatuses;
    }

    private Map<Long, Integer> initializeZeroCountMap(List<Long> ids) {
        Map<Long, Integer> unreadCounts = new LinkedHashMap<>();
        ids.forEach(id -> unreadCounts.put(id, 0));
        return unreadCounts;
    }

    private Map<Long, UnreadStatus> initializeUnreadStatusMap(List<Long> memberIds) {
        Map<Long, UnreadStatus> unreadStatuses = new LinkedHashMap<>();
        memberIds.forEach(memberId -> unreadStatuses.put(memberId, UnreadStatus.none()));
        return unreadStatuses;
    }

    public record UnreadStatus(
            boolean hasPartyUnread,
            boolean hasDirectUnread
    ) {

        public static UnreadStatus none() {
            return new UnreadStatus(false, false);
        }

        public boolean hasUnread() {
            return hasPartyUnread || hasDirectUnread;
        }
    }
}
