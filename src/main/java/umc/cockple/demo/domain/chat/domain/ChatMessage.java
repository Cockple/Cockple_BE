package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(nullable = false)
    private Boolean isDeleted;

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessageFile> chatMessageFiles = new ArrayList<>();

    public static ChatMessage create(ChatRoom chatRoom, Member sender, String content, MessageType type) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .type(type)
                .isDeleted(false)
                .build();
    }

    public String getDisplayContent() {
        if (this.content != null && !this.content.trim().isEmpty()) {
            return this.content;
        }

        if (this.chatMessageFiles != null && !this.chatMessageFiles.isEmpty()) {
            ChatMessageFile firstFile = this.chatMessageFiles.get(0);
            int count = this.chatMessageFiles.size();

            if (firstFile.getIsEmoji()) {
                return "이모티콘을 보냈습니다.";
            } else {
                return count > 1 ?
                        String.format("사진 %d장을 보냈습니다.", count) :
                        "사진을 보냈습니다.";
            }
        }

        return "메시지";
    }
}
