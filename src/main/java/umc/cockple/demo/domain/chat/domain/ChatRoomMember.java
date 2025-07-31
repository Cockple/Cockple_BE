package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

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

    private Long lastReadMessageId;

    @OneToMany(mappedBy = "chatRoomMember")
    private List<ChatMessage> chatMessages = new ArrayList<>();

}
