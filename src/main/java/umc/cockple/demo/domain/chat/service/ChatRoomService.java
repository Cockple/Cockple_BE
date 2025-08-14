package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;

    public void createChatRoom(Party party, Member owner){
        log.info("[모임 채팅방 생성 시작] - partyId: {}", party.getId());

        ChatRoom newChatRoom = chatRoomRepository.save(ChatRoom.builder()
                .party(party)
                .type(ChatRoomType.PARTY)
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .chatRoom(newChatRoom)
                .member(owner)
                .status(ChatRoomMemberStatus.JOINED)
                .build());

        log.info("[모임 채팅방 생성 완료] - chatRoomId: {}", newChatRoom.getId());
    }

    public void joinPartyChatRoom(Long partyId, Long memberId) {
        log.info("[모임 채팅방 자동 참여 시작] - memberId: {}, partyId: {}", memberId, partyId);
        Member member = findMemberOrThrow(memberId);
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId);

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .status(ChatRoomMemberStatus.JOINED)
                .build();

        chatRoomMemberRepository.save(chatRoomMember);
        log.info("[모임 채팅방 자동 참여 완료]  - chatRoomId: {}", chatRoom.getId());
    }

    public void leavePartyChatRoom(Long partyId, Long memberId) {
        log.info("[모임 채팅방 퇴장 시작] - memberId: {}, partyId:{}", memberId, partyId);
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId);

        chatRoomMemberRepository
                .findByChatRoomIdAndMemberId(chatRoom.getId(), memberId)
                .ifPresent(chatRoomMemberRepository::delete);
        log.info("[모임 채팅방 퇴장 완료] - chatRoomId: {}", chatRoom.getId());
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MEMBER_NOT_FOUND));
    }
}
