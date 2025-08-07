package umc.cockple.demo.domain.chat.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatConverter {

    // ===모임 채팅방 목록===
    public PartyChatRoomDTO.Response toPartyChatRoomListResponse(List<PartyChatRoomDTO.ChatRoomInfo> chatRoomInfos, boolean hasNext) {
        return PartyChatRoomDTO.Response.builder()
                .content(chatRoomInfos)
                .hasNext(hasNext)
                .build();
    }

    public PartyChatRoomDTO.ChatRoomInfo toPartyChatRoomInfo(ChatRoom chatRoom,
                                                             int memberCount,
                                                             int unreadCount,
                                                             PartyChatRoomDTO.LastMessageInfo lastMessageInfo,
                                                             String imgUrl) {
        return PartyChatRoomDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .partyId(chatRoom.getParty().getId())
                .partyName(chatRoom.getParty().getPartyName())
                .partyImgUrl(imgUrl)
                .memberCount(memberCount)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageInfo)
                .build();
    }

    public PartyChatRoomDTO.LastMessageInfo toPartyLastMessageInfo(ChatMessage message) {
        if (message == null) return null;

        return PartyChatRoomDTO.LastMessageInfo.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .timestamp(message.getCreatedAt())
                .messageType(message.getType().name())
                .build();
    }

    // ===개인 채팅방 목록===
    public DirectChatRoomDTO.Response toDirectChatRoomListResponse(List<DirectChatRoomDTO.ChatRoomInfo> chatRoomInfos) {
        return DirectChatRoomDTO.Response.builder()
                .content(chatRoomInfos)
                .totalElements(chatRoomInfos.size())
                .build();
    }

    public DirectChatRoomDTO.ChatRoomInfo toDirectChatRoomInfo(ChatRoom chatRoom,
                                                               ChatRoomMember chatRoomMember,
                                                               int unreadCount,
                                                               DirectChatRoomDTO.LastMessageInfo lastMessageInfo,
                                                               String imgUrl) {
        return DirectChatRoomDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .displayName(chatRoomMember.getDisplayName())
                .profileImgUrl(imgUrl)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageInfo)
                .build();
    }

    public DirectChatRoomDTO.LastMessageInfo toDirectLastMessageInfo(ChatMessage message) {
        if (message == null) return null;

        return DirectChatRoomDTO.LastMessageInfo.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .timestamp(message.getCreatedAt())
                .messageType(message.getType().name())
                .build();
    }

    // ===개인 채팅방 생성===
    public DirectChatRoomCreateDTO.Response toDirectChatRoomCreateDTO(ChatRoom chatRoom, List<ChatRoomMember> members, String displayName) {
        return DirectChatRoomCreateDTO.Response.builder()
                .chatRoomId(chatRoom.getId())
                .displayName(displayName)
                .createdAt(chatRoom.getCreatedAt())
                .members(toMemberInfo(members))
                .build();
    }

    public List<DirectChatRoomCreateDTO.MemberInfo> toMemberInfo(List<ChatRoomMember> members) {
        return members.stream()
                .map(m -> DirectChatRoomCreateDTO.MemberInfo.builder()
                        .memberId(m.getMember().getId())
                        .memberName(m.getMember().getMemberName())
                        .build()
                ).collect(Collectors.toList());
    }

    public ChatRoomDetailDTO.ChatRoomInfo toChatRoomDetailChatRoomInfo(
            ChatRoom chatRoom,
            String displayName,
            String profileImageUrl,
            int memberCount,
            Long lastReadMessageId) {
        return ChatRoomDetailDTO.ChatRoomInfo.builder()
                .chatRoomId(chatRoom.getId())
                .chatRoomType(chatRoom.getType())
                .displayName(displayName)
                .profileImageUrl(profileImageUrl)
                .memberCount(memberCount)
                .lastReadMessageId(lastReadMessageId)
                .build();
    }

    public ChatRoomDetailDTO.MessageInfo toChatRoomDetailMessageInfo(
            ChatMessage message,
            Member sender,
            String senderProfileImageUrl,
            List<String> imageUrls,
            boolean isMyMessage) {
        return ChatRoomDetailDTO.MessageInfo.builder()
                .messageId(message.getId())
                .senderId(sender.getId())
                .senderName(sender.getMemberName())
                .senderProfileImageUrl(senderProfileImageUrl)
                .content(message.getContent())
                .messageType(message.getType())
                .imageUrls(imageUrls)
                .timestamp(message.getCreatedAt())
                .isMyMessage(isMyMessage)
                .build();
    }

    public ChatRoomDetailDTO.MemberInfo toChatRoomDetailMemberInfo(Member member, String memberProfileImgUrl) {
        return ChatRoomDetailDTO.MemberInfo.builder()
                .memberId(member.getId())
                .memberName(member.getMemberName())
                .profileImgUrl(memberProfileImgUrl)
                .build();
    }

    public ChatRoomDetailDTO.Response toChatRoomDetailResponse(
            ChatRoomDetailDTO.ChatRoomInfo roomInfo,
            List<ChatRoomDetailDTO.MessageInfo> messageInfos,
            List<ChatRoomDetailDTO.MemberInfo> memberInfos) {
        return ChatRoomDetailDTO.Response.builder()
                .chatRoomInfo(roomInfo)
                .messages(messageInfos)
                .participants(memberInfos)
                .build();
    }
}
