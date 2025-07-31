package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();
}
