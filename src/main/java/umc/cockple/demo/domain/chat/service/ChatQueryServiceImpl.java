package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageImg;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.*;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;
import umc.cockple.demo.domain.party.repository.PartyRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private final ChatConverter chatConverter;
    private final ImageService imageService;

    @Override
    public PartyChatRoomDTO.Response getPartyChatRooms(Long memberId, int page, int size) {
        log.info("[모임 채팅방 목록 조회 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, pageable);
        PartyChatRoomDTO.Response response = toPartyChatRoomInfos(chatRooms, memberId);
        log.info("[모임 채팅방 목록 조회 완료]");
        return response;
    }

    @Override
    public PartyChatRoomDTO.Response searchPartyChatRoomsByName(Long memberId, String name, int page, int size) {
        log.info("[모임 채팅방 이름 검색 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.searchPartyChatRoomsByName(memberId, name, pageable);
        PartyChatRoomDTO.Response response = toPartyChatRoomInfos(chatRooms, memberId);
        log.info("[모임 채팅방 이름 검색 완료]");
        return response;
    }

    @Override
    public DirectChatRoomDTO.Response getDirectChatRooms(Long memberId, int page, int size) {
        log.info("[개인 채팅방 목록 조회 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(memberId, pageable);
        DirectChatRoomDTO.Response response = toDirectChatRoomInfos(chatRooms, memberId);
        log.info("[개인 채팅방 목록 조회 완료]");
        return response;
    }

    @Override
    public DirectChatRoomDTO.Response searchDirectChatRoomsByName(Long memberId, String name, int page, int size) {
        log.info("[개인 채팅방 이름 검색 시작]- 요청자: {}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Slice<ChatRoom> chatRooms = chatRoomRepository.searchDirectChatRoomsByName(memberId, name, pageable);
        DirectChatRoomDTO.Response response = toDirectChatRoomInfos(chatRooms, memberId);
        log.info("[개인 채팅방 이름 검색 완료]");
        return response;
    }

    @Override
    public ChatRoomDetailDTO.Response getChatRoomDetail(Long roomId, Long memberId) {
        log.info("[초기 채팅방 조회 시작] - roomId: {}, memberId: {}", roomId, memberId);

        ChatRoom chatRoom = findChatRoomWithPartyOrThrow(roomId);
        ChatRoomMember myMembership = findChatRoomMembershipOrThrow(roomId, memberId);

        ChatRoomDetailDTO.ChatRoomInfo roomInfo = buildChatRoomInfo(chatRoom, myMembership);

        Pageable pageable = PageRequest.of(0, 50);
        List<ChatMessage> recentMessages = findRecentMessagesWithImages(roomId, pageable);
        Collections.reverse(recentMessages);
        List<ChatRoomDetailDTO.MessageInfo> messageInfos = buildMessageInfos(memberId, recentMessages);

        List<ChatRoomMember> participants = findChatRoomMembersWithMemberOrThrow(roomId);
        List<ChatRoomDetailDTO.MemberInfo> memberInfos = buildMemberInfos(participants);

        if (!recentMessages.isEmpty()) {
            ChatMessage lastMessage = recentMessages.get(recentMessages.size() - 1);
            updateLastReadMessage(myMembership, lastMessage.getId());
        }

        log.info("[초기 채팅방 조회 완료] - 메시지 수: {}, 참여자 수: {}",
                messageInfos.size(), memberInfos.size());

        return chatConverter.toChatRoomDetailResponse(roomInfo, messageInfos, memberInfos);
    }

    @Override
    public ChatMessageDTO.Response getChatMessages(Long roomId, Long memberId, Long cursor, int size) {
        log.info("[채팅방 과거 메시지 조회 시작] - 채팅방 Id: {}, 멤버 Id: {}, 마지막으로 조회된 메시지 Id: {}, size: {}",
                roomId, memberId, cursor, size);

        validateChatRoomAccess(roomId, memberId);

        Pageable pageable = PageRequest.of(0, size+1);
        List<ChatMessage> messages = findMessagesWithCursor(roomId, cursor, pageable);

        boolean hasNext = messages.size() > size;
        if (hasNext) {
            messages = messages.subList(0, size);
        }

        Collections.reverse(messages);

        List<ChatMessageDTO.MessageInfo> messageInfos = buildPreviousMessageInfos(memberId, messages);

        Long nextCursor = hasNext && !messages.isEmpty()
                ? messages.get(0).getId() : null;

        log.info("[채팅방 과거 메시지 조회 완료] - 메시지 수: {}, hasNext: {}", messages.size(), hasNext);

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
            throw new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
    }

    private void validateIsMember(Long partyId, Long memberId) {
        if (!memberPartyRepository.existsByPartyIdAndMemberId(partyId, memberId)) {
            throw new ChatException(ChatErrorCode.NOT_PARTY_MEMBER);
        }
    }

    // ========== 비즈니스 로직 ==========
    private PartyChatRoomDTO.Response toPartyChatRoomInfos(Slice<ChatRoom> chatRooms, Long memberId) {
        if (chatRooms.isEmpty()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        List<PartyChatRoomDTO.ChatRoomInfo> roomInfos = chatRooms.stream()
                .map(chatRoom -> {
                    Long chatRoomId = chatRoom.getId();

                    ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                            .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

                    int memberCount = chatRoomMemberRepository.countByChatRoomId(chatRoomId);
                    Long lastReadMessageId = chatRoomMember.getLastReadMessageId();
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

        return chatConverter.toPartyChatRoomListResponse(roomInfos, chatRooms.hasNext());
    }

    private DirectChatRoomDTO.Response toDirectChatRoomInfos(Slice<ChatRoom> chatRooms, Long memberId) {
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

        return chatConverter.toDirectChatRoomListResponse(roomInfos, chatRooms.hasNext());
    }

    private ChatRoomDetailDTO.ChatRoomInfo buildChatRoomInfo(ChatRoom chatRoom, ChatRoomMember myMembership) {
        String displayName;
        String profileImageUrl = null;

        if (chatRoom.getType() == ChatRoomType.DIRECT) {
            ChatRoomMember counterPart = findCounterPartWithMemberOrThrow(chatRoom, myMembership);
            Member member = counterPart.getMember();

            displayName = member.getMemberName();
            profileImageUrl = getImageUrl(member.getProfileImg());
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
                memberCount,
                lastReadMessageId);
    }

    private List<ChatRoomDetailDTO.MessageInfo> buildMessageInfos(Long memberId, List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .map(message -> buildMessageInfo(message, memberId))
                .toList();
    }

    private List<ChatMessageDTO.MessageInfo> buildPreviousMessageInfos(Long memberId, List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .map(message -> buildPreviousMessageInfo(message, memberId))
                .toList();
    }

    private ChatRoomDetailDTO.MessageInfo buildMessageInfo(ChatMessage message, Long currentUserId) {
        Member sender = message.getSender();

        String senderProfileImageUrl = getImageUrl(sender.getProfileImg());

        List<String> imageUrls = message.getChatMessageImgs().stream()
                .sorted(Comparator.comparing(ChatMessageImg::getImgOrder))
                .map(this::getImageUrl)
                .toList();

        boolean isMyMessage = sender.getId().equals(currentUserId);

        return chatConverter.toChatRoomDetailMessageInfo(
                message,
                sender,
                senderProfileImageUrl,
                imageUrls,
                isMyMessage);
    }

    private ChatMessageDTO.MessageInfo buildPreviousMessageInfo(ChatMessage message, Long currentUserId) {
        Member sender = message.getSender();

        String senderProfileImageUrl = getImageUrl(sender.getProfileImg());

        List<String> imageUrls = message.getChatMessageImgs().stream()
                .sorted(Comparator.comparing(ChatMessageImg::getImgOrder))
                .map(this::getImageUrl)
                .toList();

        boolean isMyMessage = sender.getId().equals(currentUserId);

        return chatConverter.toPreviousMessageInfo(
                message,
                sender,
                senderProfileImageUrl,
                imageUrls,
                isMyMessage);
    }

    private List<ChatRoomDetailDTO.MemberInfo> buildMemberInfos(List<ChatRoomMember> participants) {
        return participants.stream()
                .map(this::buildMemberInfo)
                .toList();
    }

    private ChatRoomDetailDTO.MemberInfo buildMemberInfo(ChatRoomMember chatRoomMember) {
        Member member = chatRoomMember.getMember();
        String memberProfileImgUrl = getImageUrl(member.getProfileImg());

        return chatConverter.toChatRoomDetailMemberInfo(member, memberProfileImgUrl);
    }

    private void updateLastReadMessage(ChatRoomMember myMembership, Long messageId) {
        myMembership.updateLastReadMessageId(messageId);
        chatRoomMemberRepository.save(myMembership);
    }

    private String getImageUrl(PartyImg partyImg) {
        if (partyImg != null && partyImg.getImgKey() != null && !partyImg.getImgKey().isBlank()) {
            return imageService.getUrlFromKey(partyImg.getImgKey());
        }
        return null;
    }

    private String getImageUrl(ProfileImg profileImg) {
        if (profileImg == null || profileImg.getImgKey() == null) {
            return null;
        }
        return imageService.getUrlFromKey(profileImg.getImgKey());
    }

    private String getImageUrl(ChatMessageImg chatMessageImg) {
        if (chatMessageImg != null && chatMessageImg.getImgKey() != null && !chatMessageImg.getImgKey().isBlank()) {
            return imageService.getUrlFromKey(chatMessageImg.getImgKey());
        }
        return null;
    }

    // ========== 조회 메서드 ==========

    private ChatRoom findChatRoomWithPartyOrThrow(Long roomId) {
        return chatRoomRepository.findChatRoomWithPartyById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoomMember findChatRoomMembershipOrThrow(Long roomId, Long memberId) {
        return chatRoomMemberRepository
                .findByChatRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));
    }

    private ChatRoomMember findCounterPartWithMemberOrThrow(ChatRoom chatRoom, ChatRoomMember myMembership) {
        return chatRoomMemberRepository
                .findCounterPartWithMember(chatRoom.getId(), myMembership.getMember().getId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));
    }

    private List<ChatRoomMember> findChatRoomMembersWithMemberOrThrow(Long roomId) {
        return chatRoomMemberRepository.findChatRoomMembersWithMemberById(roomId);
    }

    private List<ChatMessage> findRecentMessagesWithImages(Long roomId, Pageable pageable) {
        return chatMessageRepository.findRecentMessagesWithImages(roomId, pageable);
    }

    private List<ChatMessage> findMessagesWithCursor(Long roomId, Long cursor, Pageable pageable) {
        return chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(roomId, cursor, pageable);
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.PARTY_NOT_FOUND));
    }
}
