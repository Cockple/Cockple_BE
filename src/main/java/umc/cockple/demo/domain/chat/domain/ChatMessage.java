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
    private List<ChatMessageImg> chatMessageImgs = new ArrayList<>();

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

        boolean hasImages = this.chatMessageImgs != null && !this.chatMessageImgs.isEmpty();
        boolean hasFiles = this.chatMessageFiles != null && !this.chatMessageFiles.isEmpty();

        if (hasImages && hasFiles) {
            return "사진과 파일을 보냈습니다.";
        }

        if (hasImages) {
            int imageCount = this.chatMessageImgs.size();
            return imageCount > 1 ?
                    String.format("사진 %d장을 보냈습니다.", imageCount) :
                    "사진을 보냈습니다.";
        }

        if (hasFiles) {
            int fileCount = this.chatMessageFiles.size();
            return fileCount > 1 ?
                    String.format("파일 %d개를 보냈습니다.", fileCount) :
                    "파일을 보냈습니다.";
        }

        return "메시지";
    }
}
