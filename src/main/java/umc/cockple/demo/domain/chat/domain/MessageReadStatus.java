package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "message_read_status",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"chat_message_id", "member_id"}
        ),
        indexes = {
                @Index(name = "idx_message_read_message", columnList = "chat_message_id"),
                @Index(name = "idx_message_read_member", columnList = "member_id"),
                @Index(name = "idx_message_read_chatroom_member", columnList = "chat_room_id, member_id")
        }
)
public class MessageReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private Boolean isRead;

    public static MessageReadStatus createUnread(Long chatMessageId, Long memberId, Long chatRoomId) {
        return MessageReadStatus.builder()
                .chatMessageId(chatMessageId)
                .memberId(memberId)
                .chatRoomId(chatRoomId)
                .isRead(false)
                .build();
    }

    public static MessageReadStatus createRead(Long chatMessageId, Long memberId, Long chatRoomId) {
        return MessageReadStatus.builder()
                .chatMessageId(chatMessageId)
                .memberId(memberId)
                .chatRoomId(chatRoomId)
                .isRead(true)
                .build();
    }
}
