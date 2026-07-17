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
import umc.cockple.demo.domain.chat.dto.LastMessageCacheDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.service.ChatUnreadQueryService;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.party.domain.PartyImg;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatConverter chatConverter;
    private final FileService fileService;
    private final ChatRoomListCacheService chatRoomListCacheService;

    public PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, int page, int size) {
        log.info("[모임 채팅방 목록 조회 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, pageable);
        PartyChatRoomDTO.Response response = toPartyChatRoomInfos(chatRooms, memberId);
        log.info("[모임 채팅방 목록 조회 완료]");
        return response;
    }

    public PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, int page, int size) {
        log.info("[모임 채팅방 이름 검색 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.searchPartyChatRoomsByName(memberId, name, pageable);
        PartyChatRoomDTO.Response response = toPartyChatRoomInfos(chatRooms, memberId);
        log.info("[모임 채팅방 이름 검색 완료]");
        return response;
    }

    private PartyChatRoomDTO.Response toPartyChatRoomInfos(Slice<ChatRoom> chatRooms, Long memberId) {
        if (chatRooms.isEmpty()) {
            return chatConverter.toEmptyPartyChatRoomInfos();
        }
        List<ChatRoom> chatRoomList = chatRooms.getContent();
        List<Long> chatRoomIds = chatRoomList.stream()
                .map(ChatRoom::getId)
                .toList();
        Map<Long, Integer> unreadCounts = chatUnreadQueryService.countUnreadMessagesByChatRooms(memberId, chatRoomIds);

        List<PartyChatRoomDTO.ChatRoomInfo> roomInfos = chatRoomList.stream()
                .map(chatRoom -> {
                    Long chatRoomId = chatRoom.getId();

                    chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                            .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

                    int memberCount = chatRoomMemberRepository.countByChatRoomId(chatRoomId);
                    int unreadCount = unreadCounts.getOrDefault(chatRoomId, 0);

                    LastMessageCacheDTO lastMessage = chatRoomListCacheService.getLastMessage(chatRoomId);
                    String imgUrl = getImageUrl(chatRoom.getParty().getPartyImg());

                    return chatConverter.toPartyChatRoomInfo(
                            chatRoom,
                            memberCount,
                            unreadCount,
                            chatConverter.toPartyLastMessageInfo(lastMessage),
                            imgUrl
                    );
                })
                .collect(Collectors.toList());

        return chatConverter.toPartyChatRoomListResponse(roomInfos, chatRooms.hasNext());
    }

    private String getImageUrl(PartyImg partyImg) {
        if (partyImg != null && partyImg.getImgKey() != null && !partyImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(partyImg.getImgKey());
        }
        return null;
    }
}
