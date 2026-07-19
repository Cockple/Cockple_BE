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
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.party.domain.PartyImg;

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

    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatConverter chatConverter;
    private final ImageUrlResolver imageUrlResolver;
    private final ChatProcessor chatProcessor;
    private final PartyChatRoomQueryService partyChatRoomQueryService;
    private final DirectChatRoomQueryService directChatRoomQueryService;
    private final PartyChatRoomIdQueryService partyChatRoomIdQueryService;
    private final ChatMessageHistoryQueryService chatMessageHistoryQueryService;

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
        return chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size);
    }

    @Override
    public PartyChatRoomIdDTO getChatRoomId(Long partyId, Long memberId) {
        return partyChatRoomIdQueryService.getChatRoomId(partyId, memberId);
    }

    // ========== 비즈니스 로직 ==========
    private ChatRoomDetailDTO.ChatRoomInfo buildChatRoomInfo(ChatRoom chatRoom, ChatRoomMember myMembership) {
        String displayName;
        String profileImageUrl = null;
        boolean isCounterPartWithdrawn = false;

        if (chatRoom.getType() == ChatRoomType.DIRECT) {
            ChatRoomMember counterPart = findCounterPartWithMemberOrThrow(chatRoom, myMembership);
            Member member = counterPart.getMember();
            isCounterPartWithdrawn = isUnavailableMember(member);

            displayName = isCounterPartWithdrawn ? ChatConverter.UNKNOWN_USER_NAME : member.getMemberName();
            profileImageUrl = isCounterPartWithdrawn
                    ? null
                    : imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey);
        } else {
            displayName = chatRoom.getParty().getPartyName();
            profileImageUrl = imageUrlResolver.resolve(chatRoom.getParty().getPartyImg(), PartyImg::getImgKey);
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
        String memberProfileImgUrl = isUnavailableMember(member)
                ? null
                : imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey);

        return chatConverter.toChatRoomDetailMemberInfo(member, memberProfileImgUrl);
    }

    private boolean isUnavailableMember(Member member) {
        return member == null || member.isWithdrawn();
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

}
