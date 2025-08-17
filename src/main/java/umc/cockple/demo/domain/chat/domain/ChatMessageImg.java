package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessageImg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage;

    @Column(nullable = false)
    private String imgKey;

    @Column(nullable = false)
    private Integer imgOrder;

    @Column(nullable = false)
    private String originalFileName;

    private Long fileSize;

    private String fileType;

    public static ChatMessageImg create(ChatMessage message, String imgKey, Integer imgOrder, String originalFileName, Long fileSize, String fileType) {
        return ChatMessageImg.builder()
                .chatMessage(message)
                .imgKey(imgKey)
                .imgOrder(imgOrder)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .build();
    }
}