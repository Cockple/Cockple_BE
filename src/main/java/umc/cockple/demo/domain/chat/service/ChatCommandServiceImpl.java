package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatCommandServiceImpl implements ChatCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final ChatConverter chatConverter;

    @Override
    public DirectChatRoomCreateDTO.Response createDirectChatRoom(Long memberId, Long targetMemberId) {
        log.info("[개인 채팅방 생성 시작] sender: {}, receiver: {}", memberId, targetMemberId);
        if (memberId.equals(targetMemberId)) {
            throw new ChatException(ChatErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        Member me = findMemberOrThrow(memberId);
        Member target = findMemberOrThrow(targetMemberId);
        // 이미 존재하는 1:1 채팅방 있는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository.findDirectChatRoomByMemberIds(memberId, targetMemberId);
        if (existingRoom.isPresent()) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(existingRoom.get().getId());
            log.info("===이미 존재하는 채팅방===");
            return chatConverter.toDirectChatRoomCreateDTO(existingRoom.get(), members, target.getMemberName());
        }

        ChatRoom newRoom = ChatRoom.createDirectChatRoom();
        chatRoomRepository.save(newRoom);

        ChatRoomMember member1 = ChatRoomMember.createJoined(newRoom, me, target.getMemberName());
        ChatRoomMember member2 = ChatRoomMember.createPending(newRoom, target, me.getMemberName());
        chatRoomMemberRepository.saveAll(List.of(member1, member2));

        log.info("[개인 채팅방 생성 완료] chatRoomId: {}, sender: {}, receiver: {}", newRoom.getId(), memberId, targetMemberId);
        return chatConverter.toDirectChatRoomCreateDTO(newRoom, List.of(member1, member2), target.getMemberName());
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
