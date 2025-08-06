package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 안 읽은 메시지 수 세기
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId
            AND m.id > :lastReadMessageId
            """)
    int countUnreadMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );

    ChatMessage findTop1ByChatRoom_IdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("""
            SELECT m FROM ChatMessage m 
            JOIN FETCH m.sender
            LEFT JOIN FETCH m.chatMessageImgs
            WHERE m.chatRoom.id = :chatRoomId 
            AND m.isDeleted = false
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findRecentMessagesWithImages(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            LEFT JOIN FETCH m.chatMessageImgs
            WHERE m.chatRoom.id = :chatRoomId
            AND m.id < :cursor
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findByRoomIdAndIdLessThanOrderByCreatedAtDesc(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
