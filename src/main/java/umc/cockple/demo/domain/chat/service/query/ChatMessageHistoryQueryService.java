package umc.cockple.demo.domain.chat.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.service.ChatProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageHistoryQueryService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatProcessor chatProcessor;
    private final ChatConverter chatConverter;

    public ChatMessageDTO.Response getChatMessages(Long roomId, Long memberId, Long cursor, int size) {
        log.info("[채팅방 과거 메시지 조회 시작] - 채팅방 Id: {}, 멤버 Id: {}, 마지막으로 조회된 메시지 Id: {}, size: {}",
                roomId, memberId, cursor, size);

        validateChatRoomAccess(roomId, memberId);

        Pageable pageable = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = chatMessageRepository
                .findByRoomIdAndIdLessThanOrderByCreatedAtDesc(roomId, cursor, pageable);

        boolean hasNext = messages.size() > size;
        List<ChatMessage> resultMessages = hasNext
                ? new ArrayList<>(messages.subList(0, size))
                : new ArrayList<>(messages);

        Collections.reverse(resultMessages);
        List<ChatCommonDTO.MessageInfo> commonMessages = chatProcessor.processMessages(memberId, resultMessages);
        List<ChatMessageDTO.MessageInfo> messageInfos = chatConverter.toChatMessageInfos(commonMessages);

        Long nextCursor = hasNext && !resultMessages.isEmpty()
                ? resultMessages.get(0).getId() : null;

        log.info("[채팅방 과거 메시지 조회 완료] - 메시지 수: {}, hasNext: {}", resultMessages.size(), hasNext);
        return chatConverter.toChatMessageResponse(messageInfos, hasNext, nextCursor);
    }

    private void validateChatRoomAccess(Long roomId, Long memberId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }
}
