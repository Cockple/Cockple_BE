package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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
    private String originalFileName;

    private Long fileSize;

    private String fileType;

    public static ChatMessageFile create(ChatMessage message, String originalFileName, String fileKey, Long fileSize, String fileType) {
        return ChatMessageFile.builder()
                .chatMessage(message)
                .fileKey(fileKey)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .build();
    }
}
