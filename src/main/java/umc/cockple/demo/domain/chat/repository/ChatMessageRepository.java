package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Modifying
    @Query("""
            DELETE FROM ChatMessage cm
            WHERE cm.chatRoom.id = :chatRoomId
            """)
    int deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    ChatMessage findTop1ByChatRoom_IdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("""
            SELECT m FROM ChatMessage m 
            LEFT JOIN FETCH m.sender
            LEFT JOIN FETCH m.chatMessageFiles
            WHERE m.chatRoom.id = :chatRoomId 
            AND m.isDeleted = false
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findRecentMessagesWithFiles(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);

    @Query("""
            SELECT m FROM ChatMessage m
            LEFT JOIN FETCH m.sender
            LEFT JOIN FETCH m.chatMessageFiles
            WHERE m.chatRoom.id = :chatRoomId
            AND m.isDeleted = false
            AND m.id < :cursor
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findByRoomIdAndIdLessThanOrderByCreatedAtDesc(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    int countByChatRoomId(Long chatRoomId);
}
