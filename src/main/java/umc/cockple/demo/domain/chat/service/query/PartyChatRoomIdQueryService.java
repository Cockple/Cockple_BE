package umc.cockple.demo.domain.chat.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyChatRoomIdQueryService {

    private final PartyRepository partyRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final ChatConverter chatConverter;

    public PartyChatRoomIdDTO getChatRoomId(Long partyId, Long memberId) {
        log.info("채팅방 ID 조회 시작 - partyId: {}, memberId: {}", partyId, memberId);

        Party party = findPartyOrThrow(partyId);
        validateIsMember(partyId, memberId);

        ChatRoom chatRoom = party.getChatRoom();
        if (chatRoom == null) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        log.info("채팅방 ID 조회 완료 - roomId: {}", chatRoom.getId());
        return chatConverter.toChatRoomIdDTO(chatRoom);
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.PARTY_NOT_FOUND));
    }

    private void validateIsMember(Long partyId, Long memberId) {
        if (!memberPartyRepository.existsByPartyIdAndMemberId(partyId, memberId)) {
            throw new ChatException(ChatErrorCode.NOT_PARTY_MEMBER);
        }
    }
}
