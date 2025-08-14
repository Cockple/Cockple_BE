package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String displayName;

    private Long lastReadMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomMemberStatus status = ChatRoomMemberStatus.PENDING;

    public static ChatRoomMember create(ChatRoom chatRoom, Member member) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .status(ChatRoomMemberStatus.JOINED)
                .build();
    }

    void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void updateDisplayName(String newDisplayName) {
        this.displayName = newDisplayName;
    }

    public void updateLastReadMessageId(Long messageId) {this.lastReadMessageId = messageId;}
}
