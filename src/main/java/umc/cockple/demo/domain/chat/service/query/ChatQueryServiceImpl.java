package umc.cockple.demo.domain.chat.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;

@Service
@RequiredArgsConstructor
public class ChatQueryServiceImpl implements ChatQueryService {

    private final PartyChatRoomQueryService partyChatRoomQueryService;
    private final DirectChatRoomQueryService directChatRoomQueryService;
    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatRoomDetailQueryService chatRoomDetailQueryService;
    private final ChatMessageHistoryQueryService chatMessageHistoryQueryService;
    private final PartyChatRoomIdQueryService partyChatRoomIdQueryService;

    @Override
    public PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, int page, int size) {
        return partyChatRoomQueryService.getPartyChatRooms(memberId, page, size);
    }

    @Override
    public PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, int page, int size) {
        return partyChatRoomQueryService.searchPartyChatRoomsByName(memberId, name, page, size);
    }

    @Override
    public DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, int page, int size) {
        return directChatRoomQueryService.getDirectChatRooms(memberId, page, size);
    }

    @Override
    public DirectChatRoomDTO.Response searchDirectChatRoomsByName(Long memberId, String name, int page, int size) {
        return directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, page, size);
    }

    @Override
    public ChatUnreadStatusDTO.Response getUnreadStatus(Long memberId) {
        return chatUnreadQueryService.getUnreadStatus(memberId);
    }

    @Override
    public ChatRoomDetailDTO.Response getChatRoomDetail(Long roomId, Long memberId) {
        return chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);
    }

    @Override
    public ChatMessageDTO.Response getChatMessages(Long roomId, Long memberId, Long cursor, int size) {
        return chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size);
    }

    @Override
    public PartyChatRoomIdDTO getChatRoomId(Long partyId, Long memberId) {
        return partyChatRoomIdQueryService.getChatRoomId(partyId, memberId);
    }
}
