package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;

import java.util.List;

public interface ChatFileRepository extends JpaRepository<ChatMessageFile, Long> {

    @Query("""
            SELECT cmf.fileKey FROM ChatMessageFile cmf
            WHERE cmf.chatMessage.chatRoom.id = :chatRoomId
            """)
    List<String> findObjectKeysByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM ChatMessageFile cmf
            WHERE cmf.chatMessage.chatRoom.id = :chatRoomId
            """)
    int deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
