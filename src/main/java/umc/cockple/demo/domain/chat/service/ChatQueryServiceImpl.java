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
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.enums.Direction;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.party.domain.PartyImg;

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
    private final ImageService imageService;

    @Override
    public PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, Long cursor, int size, Direction direction) {
        log.info("[모임 채팅방 목록 조회 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(0, size);
        List<ChatRoom> chatRooms = chatRoomRepository.findPartyChatRoomsByMemberId(memberId, cursor, direction.name().toLowerCase(), pageable);
        log.info("[모임 채팅방 목록 조회 완료]");
        return toPartyChatRoomInfos(chatRooms, memberId);
    }

    @Override
    public PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, Long cursor, int size, Direction direction) {
        log.info("[모임 채팅방 이름 검색 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(0, size);
        List<ChatRoom> chatRooms = chatRoomRepository.searchPartyChatRoomsByName(memberId, name, cursor, direction.name().toLowerCase(), pageable);
        log.info("[모임 채팅방 이름 검색 완료]");
        return toPartyChatRoomInfos(chatRooms, memberId);
    }

    @Override
    public DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, Long cursor, int size, Direction direction) {
        log.info("[개인 채팅방 목록 조회 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(0, size);
        List<ChatRoom> chatRooms = chatRoomRepository.findDirectChatRoomByMemberId(memberId, cursor, direction.name().toLowerCase(), pageable);
        log.info("[개인 채팅방 목록 조회 완료]");
        return toDirectChatRoomInfos(chatRooms, memberId);
    }

    /**
     * 공통 로직 메서드: 채팅방 리스트를 ChatRoomInfo 리스트로 변환
     */
    private PartyChatRoomDTO.Response toPartyChatRoomInfos(List<ChatRoom> chatRooms, Long memberId) {
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

        return chatConverter.toPartyChatRoomListResponse(roomInfos);
    }

    private String getImageUrl(PartyImg partyImg) {
        if (partyImg != null && partyImg.getImgKey() != null && !partyImg.getImgKey().isBlank()) {
            return imageService.getUrlFromKey(partyImg.getImgKey());
        }
        return null;
    }


    private DirectChatRoomDTO.Response toDirectChatRoomInfos(List<ChatRoom> chatRooms, Long memberId) {
        if (chatRooms.isEmpty()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        // 각 채팅방에 대해 ChatRoomInfo 생성
        List<DirectChatRoomDTO.ChatRoomInfo> roomInfos = chatRooms.stream()
                .map(chatRoom -> {
                    Long chatRoomId = chatRoom.getId();

                    // 나의 채팅방 참여 정보
                    ChatRoomMember myMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                            .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

                    // 상대방 찾기 (나 제외)
                    ChatRoomMember displayMember = chatRoom.getChatRoomMembers().stream()
                            .filter(crm -> !crm.getMember().getId().equals(memberId))
                            .findFirst()
                            .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

                    Long lastReadMessageId = myMember.getLastReadMessageId();
                    int unreadCount = (lastReadMessageId == null)
                            ? 0
                            : chatMessageRepository.countUnreadMessages(chatRoomId, lastReadMessageId);

                    ChatMessage lastMessage = chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(chatRoomId);

                    String displayProfileImgUrl = getImageUrl(displayMember.getMember().getProfileImg());


                    return chatConverter.toDirectChatRoomInfo(
                            chatRoom,
                            myMember,
                            unreadCount,
                            chatConverter.toDirectLastMessageInfo(lastMessage),
                            displayProfileImgUrl
                    );
                })
                .collect(Collectors.toList());

        return chatConverter.toDirectChatRoomListResponse(roomInfos);
    }

    private String getImageUrl(ProfileImg profileImg) {
        if (profileImg == null || profileImg.getImgKey() == null) {
            return null;
        }
        return imageService.getUrlFromKey(profileImg.getImgKey());
    }
}
