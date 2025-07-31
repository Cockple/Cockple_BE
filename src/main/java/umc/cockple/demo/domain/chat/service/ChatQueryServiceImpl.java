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
import umc.cockple.demo.domain.chat.enums.Direction;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;

import java.util.List;
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
    public PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, Long cursor, int size, Direction direction) {
        // 채팅방 조회 (내가 참여한 파티 채팅방)
        Pageable pageable = PageRequest.of(0, size);
        List<ChatRoom> chatRooms = chatRoomRepository.findPartyChatRoomsByMemberId(memberId, cursor, direction.name().toLowerCase(), pageable);
        if (chatRooms.isEmpty()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        // 각 채팅방에 대해 ChatRoomInfo 생성
        List<PartyChatRoomDTO.ChatRoomInfo> roomInfos = chatRooms.stream()
                .map(chatRoom -> {
                    Long chatRoomId = chatRoom.getId();
                    int memberCount = chatRoomMemberRepository.countByChatRoomId(chatRoomId);
                    ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                            .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

                    boolean isPartyMember = chatRoom.getParty().getMemberParties().stream()
                            .anyMatch(mp -> mp.getMember().equals(chatRoomMember.getMember()));
                    if (!isPartyMember) {
                        throw new ChatException(ChatErrorCode.PARTY_MEMBER_NOT_FOUND);
                    }


                    ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                            .orElseThrow(() -> new ChatException(ChatErrorCode.NOT_CHAT_ROOM_MEMBER));
                    Long lastReadMessageId = roomMember.getLastReadMessageId();
                    int unreadCount = (lastReadMessageId == null)
                            ? 0
                            : chatMessageRepository.countUnreadMessages(chatRoomId, lastReadMessageId);

                    ChatMessage lastMessage = chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(chatRoomId);

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
