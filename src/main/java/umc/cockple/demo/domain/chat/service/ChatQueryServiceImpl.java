package umc.cockple.demo.domain.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatQueryServiceImpl implements ChatQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatConverter chatConverter;

    @Override
    public PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, Long cursor, int size, String direction) {
        // 1. 해당 멤버가 속한 파티 채팅방 조회 (페이징 기반)
        Pageable pageable = PageRequest.of(0, size);
        List<ChatRoom> chatRooms = chatRoomRepository.findPartyChatRoomsByMemberId(memberId, cursor, direction, pageable);

        // 2. 각 채팅방에 대해 ChatRoomInfo 생성
        List<PartyChatRoomDTO.ChatRoomInfo> roomInfos = chatRooms.stream()
                .map(chatRoom -> {
                    Long chatRoomId = chatRoom.getId();
                    // 2-1. 채팅방 참여 인원 수
                    int memberCount = chatRoomMemberRepository.countByChatRoomId(chatRoomId);
                    // 2-2. 해당 멤버의 읽음 정보 가져오기
                    Optional<ChatRoomMember> roomMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);
                    Long lastReadMessageId = roomMember.map(ChatRoomMember::getLastReadMessageId).orElse(null);
                    // 2-3. 안 읽은 메시지 수 계산
                    int unreadCount = (lastReadMessageId == null)
                            ? 0 // 읽은 정보가 없으면 그냥 0개 처리
                            : chatMessageRepository.countUnreadMessages(chatRoomId, lastReadMessageId);
                    // 2-4. 가장 최근 메시지 가져오기
                    ChatMessage lastMessage = chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(chatRoomId);
                    // 2-5. DTO로 변환
                    return chatConverter.toChatRoomInfo(
                            chatRoom,
                            memberCount,
                            unreadCount,
                            chatConverter.toLastMessageInfo(lastMessage)
                    );
                })
                .collect(Collectors.toList());

        return chatConverter.toPartyChatRoomListResponse(roomInfos);
    }
}
