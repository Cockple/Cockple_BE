package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessageFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private Integer fileOrder;

    @Column(nullable = false)
    private String originalFileName;

    private Long fileSize;

    private String fileType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEmoji = false;

    public static ChatMessageFile create(ChatMessage message, String fileKey, Integer fileOrder, String originalFileName, Long fileSize, String fileType) {
        boolean isEmoji = isEmojiFileName(originalFileName);

        return ChatMessageFile.builder()
                .chatMessage(message)
                .fileKey(fileKey)
                .fileOrder(fileOrder)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .isEmoji(isEmoji)
                .build();
    }

    private static boolean isEmojiFileName(String originalFileName) {
        if (originalFileName == null) {
            return false;
        }
        return "emoji.png".equalsIgnoreCase(originalFileName.trim());
    }
}



