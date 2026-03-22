package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;

import java.util.List;

public class ChatFixture {

    public static ChatRoom createPartyChatRoom(Party party) {
        return ChatRoom.builder()
                .type(ChatRoomType.PARTY)
                .party(party)
                .build();
    }

    public static ChatRoom createDirectChatRoom() {
        return ChatRoom.builder()
                .type(ChatRoomType.DIRECT)
                .build();
    }

    public static ChatRoomMember createJoinedMember(ChatRoom chatRoom, Member member) {
        return ChatRoomMember.create(chatRoom, member);
    }

    public static ChatRoomMember createJoinedMember(ChatRoom chatRoom, Member member, String displayName) {
        return ChatRoomMember.createJoined(chatRoom, member, displayName);
    }

    public static ChatRoomMember createChatRoomMemberWithDisplayName(String displayName) {
        return ChatRoomMember.builder()
                .displayName(displayName)
                .build();
    }

    public static ChatRoomMember createJoinedMemberWithLastRead(ChatRoom chatRoom, Member member, Long lastReadMessageId) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .lastReadMessageId(lastReadMessageId)
                .status(ChatRoomMemberStatus.JOINED)
                .build();
    }

    public static ChatMessage createTextMessage(ChatRoom chatRoom, Member sender, String content) {
        return ChatMessage.create(chatRoom, sender, content, MessageType.TEXT);
    }

    public static ChatMessage createImageMessage(ChatRoom chatRoom, Member sender, List<ChatMessageFile> files) {
        ChatMessage message = ChatMessage.create(chatRoom, sender, null, MessageType.TEXT);
        message.getChatMessageFiles().addAll(files);
        return message;
    }

    public static ChatMessageFile createChatMessageFile(ChatMessage message, String fileKey, int fileOrder, String originalFileName) {
        return ChatMessageFile.create(message, fileKey, fileOrder, originalFileName, 1024L, "image/png");
    }
}