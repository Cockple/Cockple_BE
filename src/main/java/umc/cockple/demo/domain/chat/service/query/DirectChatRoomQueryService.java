package umc.cockple.demo.domain.chat.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.LastMessageCacheDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatConverter chatConverter;
    private final ImageUrlResolver imageUrlResolver;
    private final ChatRoomListCacheService chatRoomListCacheService;

    public DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, int page, int size) {
        log.info("[개인 채팅방 목록 조회 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, pageable);
        DirectChatRoomDTO.Response response = toDirectChatRoomInfos(chatRooms, memberId);
        log.info("[개인 채팅방 목록 조회 완료]");
        return response;
    }

    public DirectChatRoomDTO.Response searchDirectChatRoomsByName(Long memberId, String name, int page, int size) {
        log.info("[개인 채팅방 이름 검색 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.searchDirectChatRoomsByName(memberId, name, pageable);
        DirectChatRoomDTO.Response response = toDirectChatRoomInfos(chatRooms, memberId);
        log.info("[개인 채팅방 이름 검색 완료]");
        return response;
    }

    private DirectChatRoomDTO.Response toDirectChatRoomInfos(Slice<ChatRoom> chatRooms, Long memberId) {
        if (chatRooms.isEmpty()) {
            return chatConverter.toEmptyDirectChatRoomInfos();
        }
        List<ChatRoom> chatRoomList = chatRooms.getContent();
        List<Long> chatRoomIds = chatRoomList.stream()
                .map(ChatRoom::getId)
                .toList();
        Map<Long, Integer> unreadCounts = chatUnreadQueryService.countUnreadMessagesByChatRooms(memberId, chatRoomIds);

        List<DirectChatRoomDTO.ChatRoomInfo> roomInfos = chatRoomList.stream()
                .map(chatRoom -> toDirectChatRoomInfo(chatRoom, memberId, unreadCounts))
                .collect(Collectors.toList());

        return chatConverter.toDirectChatRoomListResponse(roomInfos, chatRooms.hasNext());
    }

    private DirectChatRoomDTO.ChatRoomInfo toDirectChatRoomInfo(
            ChatRoom chatRoom, Long memberId, Map<Long, Integer> unreadCounts) {
        Long chatRoomId = chatRoom.getId();

        ChatRoomMember myMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

        ChatRoomMember displayMember = chatRoom.getChatRoomMembers().stream()
                .filter(crm -> crm.getMember() == null || !crm.getMember().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

        int unreadCount = unreadCounts.getOrDefault(chatRoomId, 0);
        LastMessageCacheDTO lastMessage = chatRoomListCacheService.getLastMessage(chatRoomId);

        Member counterPartMember = displayMember.getMember();
        boolean isWithdrawn = isUnavailableMember(counterPartMember);
        String displayProfileImgUrl = isWithdrawn
                ? null
                : imageUrlResolver.resolve(counterPartMember.getProfileImg(), ProfileImg::getImgKey);

        return chatConverter.toDirectChatRoomInfo(
                chatRoom,
                myMember,
                isWithdrawn,
                unreadCount,
                chatConverter.toDirectLastMessageInfo(lastMessage),
                displayProfileImgUrl
        );
    }

    private boolean isUnavailableMember(Member member) {
        return member == null || member.isWithdrawn();
    }

}
