package umc.cockple.demo.domain.chat.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.*;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.service.ChatProcessor;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;
import umc.cockple.demo.domain.party.repository.PartyRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryServiceImpl implements ChatQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PartyRepository partyRepository;
    private final MemberPartyRepository memberPartyRepository;

    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatConverter chatConverter;
    private final FileService fileService;
    private final ChatProcessor chatProcessor;
    private final PartyChatRoomQueryService partyChatRoomQueryService;
    private final DirectChatRoomQueryService directChatRoomQueryService;

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
        log.info("[채팅 안읽음 여부 조회 시작]- 요청자: {}", memberId);
        boolean hasPartyUnread = chatUnreadQueryService.hasPartyUnreadMessages(memberId);
        boolean hasDirectUnread = chatUnreadQueryService.hasDirectUnreadMessages(memberId);
        boolean hasUnread = hasPartyUnread || hasDirectUnread;
        log.info("[채팅 안읽음 여부 조회 완료]- hasUnread: {}", hasUnread);

        return chatConverter.toUnreadStatusResponse(hasPartyUnread, hasDirectUnread);
    }

    @Override
    public ChatRoomDetailDTO.Response getChatRoomDetail(Long roomId, Long memberId) {
        log.info("[초기 채팅방 조회 시작] - roomId: {}, memberId: {}", roomId, memberId);

        ChatRoom chatRoom = findChatRoomWithPartyOrThrow(roomId);
        ChatRoomMember myMembership = findChatRoomMembershipOrThrow(roomId, memberId);

        ChatRoomDetailDTO.ChatRoomInfo roomInfo = buildChatRoomInfo(chatRoom, myMembership);

        Pageable pageable = PageRequest.of(0, 50);
        List<ChatMessage> recentMessages = findRecentMessagesWithImages(roomId, pageable);
        List<ChatMessage> resultMessages = new ArrayList<>(recentMessages);
        Collections.reverse(resultMessages);
        List<ChatCommonDTO.MessageInfo> commonMessages = chatProcessor.processMessages(memberId, resultMessages);
        List<ChatRoomDetailDTO.MessageInfo> messageInfos = chatConverter.toChatRoomDetailMessageInfos(commonMessages);

        List<ChatRoomMember> participants = findChatRoomMembersWithMemberOrThrow(roomId);
        List<ChatRoomDetailDTO.MemberInfo> memberInfos = buildMemberInfos(participants);

        log.info("[초기 채팅방 조회 완료] - 메시지 수: {}, 참여자 수: {}",
                messageInfos.size(), memberInfos.size());

        return chatConverter.toChatRoomDetailResponse(roomInfo, messageInfos, memberInfos);
    }

    @Override
    public ChatMessageDTO.Response getChatMessages(Long roomId, Long memberId, Long cursor, int size) {
        log.info("[채팅방 과거 메시지 조회 시작] - 채팅방 Id: {}, 멤버 Id: {}, 마지막으로 조회된 메시지 Id: {}, size: {}",
                roomId, memberId, cursor, size);

        validateChatRoomAccess(roomId, memberId);

        Pageable pageable = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = findMessagesWithCursor(roomId, cursor, pageable);

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

    @Override
    public PartyChatRoomIdDTO getChatRoomId(Long partyId, Long memberId) {
        log.info("채팅방 ID 조회 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임 조회
        Party party = findPartyOrThrow(partyId);

        // 해당 모임의 멤버가 맞는지 검증
        validateIsMember(partyId, memberId);

        ChatRoom chatRoom = party.getChatRoom();
        if (chatRoom == null) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        log.info("채팅방 ID 조회 완료 - roomId: {}", chatRoom.getId());

        return chatConverter.toChatRoomIdDTO(chatRoom);
    }

    // ========== 검증 로직 ==========

    private void validateChatRoomAccess(Long roomId, Long memberId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId))
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    private void validateIsMember(Long partyId, Long memberId) {
        if (!memberPartyRepository.existsByPartyIdAndMemberId(partyId, memberId)) {
            throw new ChatException(ChatErrorCode.NOT_PARTY_MEMBER);
        }
    }

    // ========== 비즈니스 로직 ==========
    private ChatRoomDetailDTO.ChatRoomInfo buildChatRoomInfo(ChatRoom chatRoom, ChatRoomMember myMembership) {
        String displayName;
        String profileImageUrl = null;
        boolean isCounterPartWithdrawn = false;

        if (chatRoom.getType() == ChatRoomType.DIRECT) {
            ChatRoomMember counterPart = findCounterPartWithMemberOrThrow(chatRoom, myMembership);
            Member member = counterPart.getMember();
            isCounterPartWithdrawn = isWithdrawn(member);

            displayName = isCounterPartWithdrawn ? ChatConverter.UNKNOWN_USER_NAME : member.getMemberName();
            profileImageUrl = isCounterPartWithdrawn ? null : getImageUrl(member.getProfileImg());
        } else {
            displayName = chatRoom.getParty().getPartyName();
            profileImageUrl = getImageUrl(chatRoom.getParty().getPartyImg());
        }

        int memberCount = chatRoomMemberRepository.countByChatRoomId(chatRoom.getId());
        Long lastReadMessageId = myMembership.getLastReadMessageId();

        return chatConverter.toChatRoomDetailChatRoomInfo(
                chatRoom,
                displayName,
                profileImageUrl,
                isCounterPartWithdrawn,
                memberCount,
                lastReadMessageId);
    }

    private List<ChatRoomDetailDTO.MemberInfo> buildMemberInfos(List<ChatRoomMember> participants) {
        return participants.stream()
                .map(this::buildMemberInfo)
                .toList();
    }

    private ChatRoomDetailDTO.MemberInfo buildMemberInfo(ChatRoomMember chatRoomMember) {
        Member member = chatRoomMember.getMember();
        String memberProfileImgUrl = isWithdrawn(member) ? null : getImageUrl(member.getProfileImg());

        return chatConverter.toChatRoomDetailMemberInfo(member, memberProfileImgUrl);
    }

    private boolean isWithdrawn(Member member) {
        return member == null || member.isWithdrawn();
    }

    private String getImageUrl(PartyImg partyImg) {
        if (partyImg != null && partyImg.getImgKey() != null && !partyImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(partyImg.getImgKey());
        }
        return null;
    }

    private String getImageUrl(ProfileImg profileImg) {
        if (profileImg == null || profileImg.getImgKey() == null) {
            return null;
        }
        return fileService.getUrlFromKey(profileImg.getImgKey());
    }

    // ========== 조회 메서드 ==========

    private ChatRoom findChatRoomWithPartyOrThrow(Long roomId) {
        return chatRoomRepository.findChatRoomWithPartyById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoomMember findChatRoomMembershipOrThrow(Long roomId, Long memberId) {
        return chatRoomMemberRepository
                .findByChatRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
    }

    private ChatRoomMember findCounterPartWithMemberOrThrow(ChatRoom chatRoom, ChatRoomMember myMembership) {
        return chatRoomMemberRepository
                .findCounterPartWithMember(chatRoom.getId(), myMembership.getMember().getId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
    }

    private List<ChatRoomMember> findChatRoomMembersWithMemberOrThrow(Long roomId) {
        return chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId);
    }

    private List<ChatMessage> findRecentMessagesWithImages(Long roomId, Pageable pageable) {
        return chatMessageRepository.findRecentMessagesWithFiles(roomId, pageable);
    }

    private List<ChatMessage> findMessagesWithCursor(Long roomId, Long cursor, Pageable pageable) {
        return chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(roomId, cursor, pageable);
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.PARTY_NOT_FOUND));
    }
}
